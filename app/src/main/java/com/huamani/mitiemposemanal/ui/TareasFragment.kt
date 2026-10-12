package com.huamani.mitiemposemanal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.huamani.mitiemposemanal.MainActivity
import com.huamani.mitiemposemanal.R
import com.huamani.mitiemposemanal.adapter.TareaAdapter
import com.huamani.mitiemposemanal.db.DatabaseHelper
import com.huamani.mitiemposemanal.databinding.FragmentTareasBinding
import com.huamani.mitiemposemanal.model.Categoria
import com.huamani.mitiemposemanal.model.Tarea

class TareasFragment : Fragment() {

    private var _binding: FragmentTareasBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: TareaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTareasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        setupRecyclerView()
        loadTareas()

        binding.fabAddTarea.setOnClickListener {
            showTareaDialog(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = TareaAdapter(
            lista = emptyList(),
            onEditClick = { tarea -> showTareaDialog(tarea) },
            onDeleteClick = { tarea -> confirmDeleteTarea(tarea) }
        )
        binding.rvTareas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTareas.adapter = adapter
    }

    fun loadTareas() {
        if (_binding == null) return
        val tareas = dbHelper.getAllTareas()
        if (tareas.isEmpty()) {
            binding.tvEmptyTareas.visibility = View.VISIBLE
            binding.rvTareas.visibility = View.GONE
        } else {
            binding.tvEmptyTareas.visibility = View.GONE
            binding.rvTareas.visibility = View.VISIBLE
        }
        adapter.updateList(tareas)
        (activity as? MainActivity)?.updateHeaderCounters()
    }

    private fun showTareaDialog(tareaAEditar: Tarea?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_tarea, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreTarea)
        val spCategorias = dialogView.findViewById<Spinner>(R.id.spCategorias)

        // Cargar categorías disponibles para el Spinner
        val categorias = dbHelper.getAllCategorias()
        if (categorias.isEmpty()) {
            Toast.makeText(requireContext(), R.string.msg_seleccionar_categoria, Toast.LENGTH_LONG).show()
            return
        }

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categorias
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategorias.adapter = spinnerAdapter

        val esEdicion = tareaAEditar != null
        if (esEdicion) {
            etNombre.setText(tareaAEditar!!.nombre)
            val selectedIndex = categorias.indexOfFirst { it.id == tareaAEditar.categoriaId }
            if (selectedIndex >= 0) {
                spCategorias.setSelection(selectedIndex)
            }
        }

        val title = if (esEdicion) getString(R.string.title_editar_tarea) else getString(R.string.title_nueva_tarea)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null)
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val categoriaSeleccionada = spCategorias.selectedItem as? Categoria

            // 1. Validación: Nombre no vacío
            if (nombre.isEmpty()) {
                Toast.makeText(requireContext(), R.string.msg_tarea_vacia, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validación: Categoría seleccionada
            if (categoriaSeleccionada == null) {
                Toast.makeText(requireContext(), R.string.msg_seleccionar_categoria, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Validación: Nombre no duplicado
            val excludeId = tareaAEditar?.id ?: -1
            if (dbHelper.existsTarea(nombre, excludeId)) {
                Toast.makeText(requireContext(), R.string.msg_tarea_duplicada, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (esEdicion) {
                dbHelper.updateTarea(tareaAEditar!!.id, nombre, categoriaSeleccionada.id)
            } else {
                dbHelper.insertTarea(nombre, categoriaSeleccionada.id)
            }

            loadTareas()
            dialog.dismiss()
        }
    }

    private fun confirmDeleteTarea(tarea: Tarea) {
        val mensaje = getString(R.string.msg_confirmar_eliminar_tarea, tarea.nombre)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_confirmacion)
            .setMessage(mensaje)
            .setPositiveButton(R.string.btn_eliminar) { _, _ ->
                dbHelper.deleteTarea(tarea.id)
                loadTareas()
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
