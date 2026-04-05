package com.nulstudio.kwoocollector.net.model.response

import kotlinx.serialization.Serializable

@Serializable
data class EntryAbstractResponse(
    val id: Int,
    val primary: String,
    val secondary: List<String>
)
