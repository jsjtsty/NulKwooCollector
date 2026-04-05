package com.nulstudio.kwoocollector.net.model.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class FormHistoryDetailResponse(
    val formId: Int,
    val title: String,
    val description: String,
    val submittedAt: Long,
    val fields: List<FormField>,
    val content: JsonObject
)
