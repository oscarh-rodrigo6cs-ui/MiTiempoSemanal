package com.huamani.mitiemposemanal.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.huamani.mitiemposemanal.MainActivity
import com.huamani.mitiemposemanal.R
import com.huamani.mitiemposemanal.adapter.EventoAdapter
import com.huamani.mitiemposemanal.databinding.FragmentEventosBinding
import com.huamani.mitiemposemanal.db.DatabaseHelper
import com.huamani.mitiemposemanal.model.Categoria
import com.huamani.mitiemposemanal.model.Evento
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EventosFragment : Fragment() {

    private var _binding: FragmentEventosBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var eventoAdapter: EventoAdapter

    private var eventoEnCurso: Evento? = null
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    private val handler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            updateCronometro()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        setupRecyclerView()
        setupSearchAutoComplete()
        setupButtons()
        checkEstadoEvento()
        loadHistorial()
    }

    private fun setupRecyclerView() {
        eventoAdapter = EventoAdapter(emptyList())
        binding.rvEventos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEventos.adapter = eventoAdapter
    }

    private fun setupSearchAutoComplete() {
        binding.actvBuscarTarea.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                if (query.length >= 2) {
                    val tareasEncontradas = dbHelper.searchTareas(query)
                    val nombres = tareasEncontradas.map { it.nombre }
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        nombres
                    )
                    binding.actvBuscarTarea.setAdapter(adapter)
                    binding.actvBuscarTarea.showDropDown()
                }

                // Detectar si la tarea ya existe para mostrar su categoría
                val tareaExistente = dbHelper.getTareaByNombre(query)
                if (tareaExistente != null) {
                    binding.tvCategoriaSeleccionada.text = "Categoría: ${tareaExistente.categoriaNombre}"
                    binding.tvCategoriaSeleccionada.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.primary)
                    )
                } else {
                    binding.tvCategoriaSeleccionada.text = "Categoría: (Nueva tarea - Se pedirá categoría al iniciar)"
                    binding.tvCategoriaSeleccionada.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_secondary)
                    )
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.actvBuscarTarea.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as? String
            if (selectedName != null) {
                val tarea = dbHelper.getTareaByNombre(selectedName)
                if (tarea != null) {
                    binding.tvCategoriaSeleccionada.text = "Categoría: ${tarea.categoriaNombre}"
                }
            }
        }
    }

    private fun setupButtons() {
        binding.btnIniciar.setOnClickListener {
            val tareaNombre = binding.actvBuscarTarea.text.toString().trim()
            if (tareaNombre.isEmpty()) {
                Toast.makeText(requireContext(), R.string.msg_ingresar_nombre_tarea, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verificar si hay evento en curso
            if (dbHelper.getEventoEnCurso() != null) {
                Toast.makeText(requireContext(), R.string.msg_evento_ya_iniciado, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val tarea = dbHelper.getTareaByNombre(tareaNombre)
            if (tarea == null) {
                // HU-04: La tarea no existe, solicitar categoría
                mostrarDialogoCrearTarea(tareaNombre) { tareaCreadaId ->
                    iniciarEventoConTareaId(tareaCreadaId)
                }
            } else {
                iniciarEventoConTareaId(tarea.id)
            }
        }

        binding.btnFinalizar.setOnClickListener {
            val eventoActivo = dbHelper.getEventoEnCurso()
            if (eventoActivo != null) {
                val finStr = LocalDateTime.now().format(formatter)
                val inicioTime = LocalDateTime.parse(eventoActivo.inicio, formatter)
                val finTime = LocalDateTime.parse(finStr, formatter)

                val duration = Duration.between(inicioTime, finTime)
                val horas = duration.toHours()
                val minutos = duration.toMinutes() % 60
                val segundos = duration.seconds % 60
                val duracionStr = String.format("%02d:%02d:%02d", horas, minutos, segundos)

                dbHelper.finalizarEvento(eventoActivo.id, finStr, duracionStr)
                Toast.makeText(requireContext(), R.string.msg_evento_finalizado, Toast.LENGTH_SHORT).show()

                stopCronometro()
                checkEstadoEvento()
                loadHistorial()
            }
        }
    }

    private fun mostrarDialogoCrearTarea(nombreTarea: String, onTareaCreada: (Int) -> Unit) {
        val categorias = dbHelper.getAllCategorias()
        if (categorias.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_seleccionar_categoria, Toast.LENGTH_LONG).show()
            return
        }

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_crear_tarea_evento, null)
        val tvMensaje = dialogView.findViewById<TextView>(R.id.tvMensajeCrearTarea)
        val spCat = dialogView.findViewById<Spinner>(R.id.spCategoriasNuevaTarea)

        tvMensaje.text = getString(R.string.msg_confirmar_crear_tarea, nombreTarea)

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categorias
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCat.adapter = spinnerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_nueva_tarea)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar) { _, _ ->
                val catSeleccionada = spCat.selectedItem as? Categoria
                if (catSeleccionada != null) {
                    val nuevoId = dbHelper.insertTarea(nombreTarea, catSeleccionada.id)
                    if (nuevoId > 0) {
                        // Notificar a MainActivity para actualizar la pestaña de Tareas
                        (activity as? MainActivity)?.pagerAdapter?.tareasFragment?.loadTareas()
                        (activity as? MainActivity)?.updateHeaderCounters()
                        onTareaCreada(nuevoId.toInt())
                    } else {
                        Toast.makeText(requireContext(), R.string.msg_tarea_duplicada, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    private fun iniciarEventoConTareaId(tareaId: Int) {
        val inicioStr = LocalDateTime.now().format(formatter)
        val eventoId = dbHelper.insertEvento(tareaId, inicioStr)
        if (eventoId > 0) {
            Toast.makeText(requireContext(), R.string.msg_evento_iniciado, Toast.LENGTH_SHORT).show()
            binding.actvBuscarTarea.setText("")
            binding.tvCategoriaSeleccionada.text = "Categoría: -"
            checkEstadoEvento()
            loadHistorial()
        }
    }

    fun checkEstadoEvento() {
        if (_binding == null) return
        eventoEnCurso = dbHelper.getEventoEnCurso()
        if (eventoEnCurso != null) {
            binding.layoutEventoActivo.visibility = View.VISIBLE
            binding.tvActivoTarea.text = "${eventoEnCurso!!.tareaNombre} (${eventoEnCurso!!.categoriaNombre})"
            binding.btnIniciar.isEnabled = false
            binding.btnFinalizar.isEnabled = true
            binding.actvBuscarTarea.isEnabled = false
            startCronometro()
        } else {
            binding.layoutEventoActivo.visibility = View.GONE
            binding.btnIniciar.isEnabled = true
            binding.btnFinalizar.isEnabled = false
            binding.actvBuscarTarea.isEnabled = true
            stopCronometro()
        }
    }

    private fun startCronometro() {
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
    }

    private fun stopCronometro() {
        handler.removeCallbacks(timerRunnable)
        binding.tvActivoCronometro.text = "00:00:00"
    }

    private fun updateCronometro() {
        if (eventoEnCurso == null) return
        try {
            val inicioTime = LocalDateTime.parse(eventoEnCurso!!.inicio, formatter)
            val now = LocalDateTime.now()
            val duration = Duration.between(inicioTime, now)
            val horas = duration.toHours()
            val minutos = duration.toMinutes() % 60
            val segundos = duration.seconds % 60
            binding.tvActivoCronometro.text = String.format("%02d:%02d:%02d", horas, minutos, segundos)
        } catch (_: Exception) {
            binding.tvActivoCronometro.text = "00:00:00"
        }
    }

    fun loadHistorial() {
        if (_binding == null) return
        val eventos = dbHelper.getAllEventos()
        if (eventos.isEmpty()) {
            binding.tvEmptyEventos.visibility = View.VISIBLE
            binding.rvEventos.visibility = View.GONE
        } else {
            binding.tvEmptyEventos.visibility = View.GONE
            binding.rvEventos.visibility = View.VISIBLE
        }
        eventoAdapter.updateList(eventos)
    }

    override fun onResume() {
        super.onResume()
        checkEstadoEvento()
        loadHistorial()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCronometro()
        _binding = null
    }
}
