package com.huamani.mitiemposemanal.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.huamani.mitiemposemanal.R
import com.huamani.mitiemposemanal.databinding.ItemTareaBinding
import com.huamani.mitiemposemanal.model.Tarea

class TareaAdapter(
    private var lista: List<Tarea>,
    private val onEditClick: (Tarea) -> Unit,
    private val onDeleteClick: (Tarea) -> Unit
) : RecyclerView.Adapter<TareaAdapter.TareaViewHolder>() {

    class TareaViewHolder(val binding: ItemTareaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TareaViewHolder {
        val binding = ItemTareaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TareaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TareaViewHolder, position: Int) {
        val item = lista[position]
        val context = holder.itemView.context

        // Formato requerido: "Nombre Tarea · Categoría"
        val textoFormateado = "${item.nombre} · ${item.categoriaNombre}"
        holder.binding.tvTareaFormateada.text = textoFormateado
        holder.binding.tvBadgeCategoria.text = item.categoriaNombre

        // Personalización de color según la categoría
        val (bgColorRes, textColorRes) = when (item.categoriaNombre.trim().lowercase()) {
            "estudio" -> Pair(R.color.cat_estudio_bg, R.color.cat_estudio_text)
            "trabajo" -> Pair(R.color.cat_trabajo_bg, R.color.cat_trabajo_text)
            "descanso" -> Pair(R.color.cat_descanso_bg, R.color.cat_descanso_text)
            "ocio" -> Pair(R.color.cat_ocio_bg, R.color.cat_ocio_text)
            else -> Pair(R.color.cat_default_bg, R.color.cat_default_text)
        }

        val bgColor = ContextCompat.getColor(context, bgColorRes)
        val textColor = ContextCompat.getColor(context, textColorRes)

        holder.binding.tvBadgeCategoria.backgroundTintList = ColorStateList.valueOf(bgColor)
        holder.binding.tvBadgeCategoria.setTextColor(textColor)

        holder.binding.btnEditTarea.setOnClickListener { onEditClick(item) }
        holder.binding.btnDeleteTarea.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = lista.size

    fun updateList(newList: List<Tarea>) {
        lista = newList
        notifyDataSetChanged()
    }
}
