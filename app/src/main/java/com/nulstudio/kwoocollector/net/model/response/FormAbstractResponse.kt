package com.nulstudio.kwoocollector.net.model.response

import kotlinx.serialization.Serializable

@Serializable
data class FormAbstractResponse(
    val id: Int,
    val name: String,
    val deliverTime: Long,
    val deadline: Long,
    val priority: Int
)
