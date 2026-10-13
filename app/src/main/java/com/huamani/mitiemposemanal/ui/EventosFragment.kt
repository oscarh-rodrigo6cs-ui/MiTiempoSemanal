package com.huamani.mitiemposemanal.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.huamani.mitiemposemanal.MainActivity
import com.huamani.mitiemposemanal.R
import com.huamani.mitiemposemanal.adapter.EventoAdapter
import com.huamani.mitiemposemanal.db.DatabaseHelper
import com.huamani.mitiemposemanal.model.Categoria
import com.huamani.mitiemposemanal.model.Evento
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class EventosFragment : Fragment() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var eventoAdapter: EventoAdapter

    private var eventoEnCurso: Evento? = null
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    // Referencias de vistas (findViewById)
    private lateinit var actvBuscarTarea: AutoCompleteTextView
    private lateinit var tvCategoriaSeleccionada: TextView
    private lateinit var layoutEventoActivo: LinearLayout
    private lateinit var tvActivoTarea: TextView
    private lateinit var tvActivoCronometro: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnFinalizar: Button
    private lateinit var tvRangoSemana: TextView
    private lateinit var llResumenContainer: LinearLayout
    private lateinit var tvEmptyEventos: TextView
    private lateinit var rvEventos: RecyclerView

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
        val view = inflater.inflate(R.layout.fragment_eventos, container, false)

        actvBuscarTarea = view.findViewById(R.id.actvBuscarTarea)
        tvCategoriaSeleccionada = view.findViewById(R.id.tvCategoriaSeleccionada)
        layoutEventoActivo = view.findViewById(R.id.layoutEventoActivo)
        tvActivoTarea = view.findViewById(R.id.tvActivoTarea)
        tvActivoCronometro = view.findViewById(R.id.tvActivoCronometro)
        btnIniciar = view.findViewById(R.id.btnIniciar)
        btnFinalizar = view.findViewById(R.id.btnFinalizar)
        tvRangoSemana = view.findViewById(R.id.tvRangoSemana)
        llResumenContainer = view.findViewById(R.id.llResumenContainer)
        tvEmptyEventos = view.findViewById(R.id.tvEmptyEventos)
        rvEventos = view.findViewById(R.id.rvEventos)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        setupRecyclerView()
        setupSearchAutoComplete()
        setupButtons()
        checkEstadoEvento()
        loadHistorial()
        loadResumenSemanal()
    }

    private fun setupRecyclerView() {
        eventoAdapter = EventoAdapter(emptyList())
        rvEventos.layoutManager = LinearLayoutManager(requireContext())
        rvEventos.adapter = eventoAdapter
    }

    private fun setupSearchAutoComplete() {
        actvBuscarTarea.addTextChangedListener(object : TextWatcher {
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
                    actvBuscarTarea.setAdapter(adapter)
                    actvBuscarTarea.showDropDown()
                }

                val tareaExistente = dbHelper.getTareaByNombre(query)
                if (tareaExistente != null) {
                    tvCategoriaSeleccionada.text = "Categoría: ${tareaExistente.categoriaNombre}"
                    tvCategoriaSeleccionada.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.primary)
                    )
                } else {
                    tvCategoriaSeleccionada.text = "Categoría: (Nueva tarea - Se pedirá categoría al iniciar)"
                    tvCategoriaSeleccionada.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.text_secondary)
                    )
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        actvBuscarTarea.setOnItemClickListener { parent, _, position, _ ->
            val selectedName = parent.getItemAtPosition(position) as? String
            if (selectedName != null) {
                val tarea = dbHelper.getTareaByNombre(selectedName)
                if (tarea != null) {
                    tvCategoriaSeleccionada.text = "Categoría: ${tarea.categoriaNombre}"
                }
            }
        }
    }

    private fun setupButtons() {
        btnIniciar.setOnClickListener {
            val tareaNombre = actvBuscarTarea.text.toString().trim()
            if (tareaNombre.isEmpty()) {
                Toast.makeText(requireContext(), R.string.msg_ingresar_nombre_tarea, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (dbHelper.getEventoEnCurso() != null) {
                Toast.makeText(requireContext(), R.string.msg_evento_ya_iniciado, Toast.LENGTH_LONG).show()
                btnIniciar.isEnabled = false
                return@setOnClickListener
            }

            val tarea = dbHelper.getTareaByNombre(tareaNombre)
            if (tarea == null) {
                mostrarDialogoCrearTarea(tareaNombre) { tareaCreadaId ->
                    iniciarEventoConTareaId(tareaCreadaId)
                }
            } else {
                iniciarEventoConTareaId(tarea.id)
            }
        }

        btnFinalizar.setOnClickListener {
            val eventoActivo = dbHelper.getEventoEnCurso()
            if (eventoActivo != null) {
                val finStr = LocalDateTime.now().format(formatter)
                val inicioTime = LocalDateTime.parse(eventoActivo.inicio, formatter)
                val finTime = LocalDateTime.parse(finStr, formatter)

                val duration = Duration.between(inicioTime, finTime)
                val totalMinutosFraccional = duration.toMillis() / 60000.0
                val horas = duration.toHours()
                val minutos = duration.toMinutes() % 60
                val segundos = duration.seconds % 60
                val duracionStr = String.format(Locale.US, "%02d:%02d:%02d", horas, minutos, segundos)

                dbHelper.finalizarEvento(eventoActivo.id, finStr, duracionStr, totalMinutosFraccional)
                Toast.makeText(requireContext(), R.string.msg_evento_finalizado, Toast.LENGTH_SHORT).show()

                stopCronometro()
                checkEstadoEvento()
                loadHistorial()
                loadResumenSemanal()
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
            actvBuscarTarea.setText("")
            tvCategoriaSeleccionada.text = "Categoría: -"
            checkEstadoEvento()
            loadHistorial()
            loadResumenSemanal()
        }
    }

    fun checkEstadoEvento() {
        if (!isAdded) return
        eventoEnCurso = dbHelper.getEventoEnCurso()
        if (eventoEnCurso != null) {
            layoutEventoActivo.visibility = View.VISIBLE
            tvActivoTarea.text = "${eventoEnCurso!!.tareaNombre} (${eventoEnCurso!!.categoriaNombre})"
            btnIniciar.isEnabled = false
            btnFinalizar.isEnabled = true
            actvBuscarTarea.isEnabled = false
            startCronometro()
        } else {
            layoutEventoActivo.visibility = View.GONE
            btnIniciar.isEnabled = true
            btnFinalizar.isEnabled = false
            actvBuscarTarea.isEnabled = true
            stopCronometro()
        }
    }

    private fun startCronometro() {
        handler.removeCallbacks(timerRunnable)
        handler.post(timerRunnable)
    }

    private fun stopCronometro() {
        handler.removeCallbacks(timerRunnable)
        tvActivoCronometro.text = "00:00:00"
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
            tvActivoCronometro.text = String.format(Locale.US, "%02d:%02d:%02d", horas, minutos, segundos)
        } catch (_: Exception) {
            tvActivoCronometro.text = "00:00:00"
        }
    }

    fun loadHistorial() {
        if (!isAdded) return
        val eventos = dbHelper.getAllEventos()
        if (eventos.isEmpty()) {
            tvEmptyEventos.visibility = View.VISIBLE
            rvEventos.visibility = View.GONE
        } else {
            tvEmptyEventos.visibility = View.GONE
            rvEventos.visibility = View.VISIBLE
        }
        eventoAdapter.updateList(eventos)
    }

    private fun formatTiempo(minutosTotales: Double): String {
        val totalSegundos = (minutosTotales * 60).toLong()
        val horas = totalSegundos / 3600
        val minutos = (totalSegundos % 3600) / 60
        val segundos = totalSegundos % 60

        return when {
            horas > 0 && minutos > 0 -> "${horas}h ${minutos}m"
            horas > 0 -> "${horas}h"
            minutos > 0 && segundos > 0 -> "${minutos}m ${segundos}s"
            minutos > 0 -> "${minutos}m"
            segundos > 0 -> "${segundos}s"
            else -> "0m"
        }
    }

    fun loadResumenSemanal() {
        if (!isAdded) return

        val now = LocalDateTime.now()
        val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toLocalDate().atStartOfDay()
        val sunday = now.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).toLocalDate().atTime(23, 59, 59)

        val startStr = monday.format(formatter)
        val endStr = sunday.format(formatter)

        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        tvRangoSemana.text = "Semana ${monday.format(dateFormatter)} - ${sunday.format(dateFormatter)}"

        val resumenList = dbHelper.getResumenSemanal(startStr, endStr)
        val totalMinutosSemana = 10080.0

        llResumenContainer.removeAllViews()

        var totalRegistradosMinutos = 0.0

        for (resumen in resumenList) {
            totalRegistradosMinutos += resumen.totalMinutos
            val tiempoFormateado = formatTiempo(resumen.totalMinutos)
            val porcentaje = (resumen.totalMinutos / totalMinutosSemana) * 100.0

            val rowView = LayoutInflater.from(requireContext()).inflate(R.layout.item_resumen_semana, llResumenContainer, false)
            val tvNombre = rowView.findViewById<TextView>(R.id.tvResumenNombre)
            val tvStats = rowView.findViewById<TextView>(R.id.tvResumenStats)
            val pb = rowView.findViewById<ProgressBar>(R.id.pbResumen)

            tvNombre.text = resumen.categoriaNombre
            tvStats.text = String.format(Locale.US, "%s (%.1f%%)", tiempoFormateado, porcentaje)
            pb.progress = porcentaje.toInt().coerceIn(0, 100)

            llResumenContainer.addView(rowView)
        }

        val sinRegistrarMinutos = (totalMinutosSemana - totalRegistradosMinutos).coerceAtLeast(0.0)
        val tiempoSinRegistrar = formatTiempo(sinRegistrarMinutos)
        val porcentajeSinRegistrar = (sinRegistrarMinutos / totalMinutosSemana) * 100.0

        val rowSinRegistrar = LayoutInflater.from(requireContext()).inflate(R.layout.item_resumen_semana, llResumenContainer, false)
        val tvNombreSr = rowSinRegistrar.findViewById<TextView>(R.id.tvResumenNombre)
        val tvStatsSr = rowSinRegistrar.findViewById<TextView>(R.id.tvResumenStats)
        val pbSr = rowSinRegistrar.findViewById<ProgressBar>(R.id.pbResumen)

        tvNombreSr.text = getString(R.string.label_sin_registrar)
        tvStatsSr.text = String.format(Locale.US, "%s (%.1f%%)", tiempoSinRegistrar, porcentajeSinRegistrar)
        pbSr.progress = porcentajeSinRegistrar.toInt().coerceIn(0, 100)
        pbSr.progressTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_secondary))

        llResumenContainer.addView(rowSinRegistrar)
    }

    override fun onResume() {
        super.onResume()
        checkEstadoEvento()
        loadHistorial()
        loadResumenSemanal()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopCronometro()
    }
}
