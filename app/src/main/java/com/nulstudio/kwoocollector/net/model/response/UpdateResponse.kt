package com.nulstudio.kwoocollector.net.model.response

import kotlinx.serialization.Serializable

@Serializable
data class UpdateResponse(
    val build: Int,
    val version: String,
    val url: String
) {
}