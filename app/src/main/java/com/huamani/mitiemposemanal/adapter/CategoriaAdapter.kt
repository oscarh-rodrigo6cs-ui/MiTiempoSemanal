package com.huamani.mitiemposemanal.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.huamani.mitiemposemanal.R
import com.huamani.mitiemposemanal.databinding.ItemCategoriaBinding
import com.huamani.mitiemposemanal.model.Categoria

class CategoriaAdapter(
    private var lista: List<Categoria>,
    private val onEditClick: (Categoria) -> Unit,
    private val onDeleteClick: (Categoria) -> Unit
) : RecyclerView.Adapter<CategoriaAdapter.CategoriaViewHolder>() {

    class CategoriaViewHolder(val binding: ItemCategoriaBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
        val binding = ItemCategoriaBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoriaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
        val item = lista[position]
        val context = holder.itemView.context

        holder.binding.tvNombreCategoria.text = item.nombre

        // Color del badge según la categoría
        val (bgColorRes, iconColorRes) = when (item.nombre.trim().lowercase()) {
            "estudio" -> Pair(R.color.cat_estudio_bg, R.color.cat_estudio_text)
            "trabajo" -> Pair(R.color.cat_trabajo_bg, R.color.cat_trabajo_text)
            "descanso" -> Pair(R.color.cat_descanso_bg, R.color.cat_descanso_text)
            "ocio" -> Pair(R.color.cat_ocio_bg, R.color.cat_ocio_text)
            else -> Pair(R.color.cat_default_bg, R.color.primary)
        }

        val bgColor = ContextCompat.getColor(context, bgColorRes)
        val iconColor = ContextCompat.getColor(context, iconColorRes)

        holder.binding.ivCatIcon.backgroundTintList = ColorStateList.valueOf(bgColor)
        holder.binding.ivCatIcon.imageTintList = ColorStateList.valueOf(iconColor)

        holder.binding.btnEditCategoria.setOnClickListener { onEditClick(item) }
        holder.binding.btnDeleteCategoria.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = lista.size

    fun updateList(newList: List<Categoria>) {
        lista = newList
        notifyDataSetChanged()
    }
}
