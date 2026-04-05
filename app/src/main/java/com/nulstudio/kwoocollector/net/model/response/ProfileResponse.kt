package com.nulstudio.kwoocollector.net.model.response

import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    val username: String,
    val role: String
)
