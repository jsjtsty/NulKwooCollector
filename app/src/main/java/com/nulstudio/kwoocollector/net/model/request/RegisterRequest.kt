package com.nulstudio.kwoocollector.net.model.request

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val username: String,
    val password: String,
    val inviteCode: String
)
