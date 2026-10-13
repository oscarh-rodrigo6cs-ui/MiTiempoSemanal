package com.huamani.mitiemposemanal.model

data class Evento(
    val id: Int,
    val tareaId: Int,
    val tareaNombre: String,
    val categoriaNombre: String,
    val inicio: String,
    val fin: String?,
    val duracion: String?
)
