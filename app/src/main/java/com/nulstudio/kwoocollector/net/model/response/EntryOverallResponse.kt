package com.nulstudio.kwoocollector.net.model.response

import kotlinx.serialization.Serializable

@Serializable
data class EntryOverallResponse(
    val count: Int,
    val abstracts: List<EntryAbstractResponse>
)
