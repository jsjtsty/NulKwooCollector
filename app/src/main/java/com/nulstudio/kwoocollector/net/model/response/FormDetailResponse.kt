package com.nulstudio.kwoocollector.net.model.response

import kotlinx.serialization.Serializable

@Serializable
data class FormDetailResponse(
    val formId: Int,
    val title: String,
    val description: String,
    val fields: List<FormField>
)
