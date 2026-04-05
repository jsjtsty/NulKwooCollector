package com.nulstudio.kwoocollector.net.model.response

import kotlinx.serialization.Serializable

@Serializable
data class TableAbstractResponse(
    val id: Int,
    val name: String,
    val schema: List<FormField>
)
