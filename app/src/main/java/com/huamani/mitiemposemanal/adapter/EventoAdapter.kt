package com.huamani.mitiemposemanal.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.huamani.mitiemposemanal.R
import com.huamani.mitiemposemanal.databinding.ItemEventoBinding
import com.huamani.mitiemposemanal.model.Evento

class EventoAdapter(
    private var lista: List<Evento>
) : RecyclerView.Adapter<EventoAdapter.EventoViewHolder>() {

    class EventoViewHolder(val binding: ItemEventoBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventoViewHolder {
        val binding = ItemEventoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventoViewHolder, position: Int) {
        val evento = lista[position]
        val context = holder.itemView.context

        holder.binding.tvTareaNombre.text = evento.tareaNombre
        holder.binding.tvCategoriaNombre.text = "Categoría: ${evento.categoriaNombre}"
        holder.binding.tvInicioValor.text = evento.inicio ?: "-"

        if (evento.fin != null) {
            holder.binding.tvFinValor.text = evento.fin
            holder.binding.tvDuracionValor.text = evento.duracion ?: "00:00:00"
            holder.binding.tvEstadoEvento.text = "Finalizado"

            // Fondo plomo/gris claro (#E0E0E0 o #F1F5F9) con texto gris oscuro (#616161)
            holder.binding.tvEstadoEvento.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.card_border)
            )
            holder.binding.tvEstadoEvento.setTextColor(
                ContextCompat.getColor(context, R.color.text_secondary)
            )
        } else {
            holder.binding.tvFinValor.text = "En curso..."
            holder.binding.tvDuracionValor.text = "Activo"
            holder.binding.tvEstadoEvento.text = "En curso"

            // Fondo verde claro (#E8F5E9) con texto verde oscuro (#2E7D32)
            holder.binding.tvEstadoEvento.backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.cat_descanso_bg)
            )
            holder.binding.tvEstadoEvento.setTextColor(
                ContextCompat.getColor(context, R.color.cat_descanso_text)
            )
        }
    }

    override fun getItemCount(): Int = lista.size

    fun updateList(newList: List<Evento>) {
        lista = newList
        notifyDataSetChanged()
    }
}
