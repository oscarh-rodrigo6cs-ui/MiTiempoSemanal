package com.huamani.mitiemposemanal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.huamani.mitiemposemanal.MainActivity
import com.huamani.mitiemposemanal.R
import com.huamani.mitiemposemanal.adapter.CategoriaAdapter
import com.huamani.mitiemposemanal.db.DatabaseHelper
import com.huamani.mitiemposemanal.databinding.FragmentCategoriasBinding
import com.huamani.mitiemposemanal.model.Categoria

class CategoriasFragment : Fragment() {

    private var _binding: FragmentCategoriasBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: CategoriaAdapter

    // Callback para notificar cambios en categorías a otros fragmentos/actividad
    var onCategoriasChangedListener: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoriasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        setupRecyclerView()
        loadCategorias()

        binding.fabAddCategoria.setOnClickListener {
            showCategoriaDialog(null)
        }
    }

    private fun setupRecyclerView() {
        adapter = CategoriaAdapter(
            lista = emptyList(),
            onEditClick = { categoria -> showCategoriaDialog(categoria) },
            onDeleteClick = { categoria -> confirmDeleteCategoria(categoria) }
        )
        binding.rvCategorias.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategorias.adapter = adapter
    }

    fun loadCategorias() {
        if (_binding == null) return
        val categorias = dbHelper.getAllCategorias()
        if (categorias.isEmpty()) {
            binding.tvEmptyCategorias.visibility = View.VISIBLE
            binding.rvCategorias.visibility = View.GONE
        } else {
            binding.tvEmptyCategorias.visibility = View.GONE
            binding.rvCategorias.visibility = View.VISIBLE
        }
        adapter.updateList(categorias)
        (activity as? MainActivity)?.updateHeaderCounters()
    }

    private fun showCategoriaDialog(categoriaAEditar: Categoria?) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_categoria, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etNombreCategoria)

        val esEdicion = categoriaAEditar != null
        if (esEdicion) {
            etNombre.setText(categoriaAEditar!!.nombre)
        }

        val title = if (esEdicion) getString(R.string.title_editar_categoria) else getString(R.string.title_nueva_categoria)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(R.string.btn_guardar, null) // Set Null to override listener and avoid auto-closing on validation error
            .setNegativeButton(R.string.btn_cancelar, null)
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()

            // 1. Validación: Campo no vacío
            if (nombre.isEmpty()) {
                Toast.makeText(requireContext(), R.string.msg_categoria_vacia, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validación: Nombre no duplicado (sensible/insensible a mayúsculas)
            val excludeId = categoriaAEditar?.id ?: -1
            if (dbHelper.existsCategoria(nombre, excludeId)) {
                Toast.makeText(requireContext(), R.string.msg_categoria_duplicada, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (esEdicion) {
                dbHelper.updateCategoria(categoriaAEditar!!.id, nombre)
            } else {
                dbHelper.insertCategoria(nombre)
            }

            loadCategorias()
            onCategoriasChangedListener?.invoke()
            dialog.dismiss()
        }
    }

    private fun confirmDeleteCategoria(categoria: Categoria) {
        // Validación obligatoria: Si tiene tareas asociadas, impedir eliminación
        if (dbHelper.hasTareasAsociadas(categoria.id)) {
            Toast.makeText(requireContext(), R.string.msg_categoria_con_tareas, Toast.LENGTH_LONG).show()
            return
        }

        val mensaje = getString(R.string.msg_confirmar_eliminar_cat, categoria.nombre)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.title_confirmacion)
            .setMessage(mensaje)
            .setPositiveButton(R.string.btn_eliminar) { _, _ ->
                val ok = dbHelper.deleteCategoria(categoria.id)
                if (ok) {
                    loadCategorias()
                    onCategoriasChangedListener?.invoke()
                } else {
                    Toast.makeText(requireContext(), R.string.msg_categoria_con_tareas, Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton(R.string.btn_cancelar, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
