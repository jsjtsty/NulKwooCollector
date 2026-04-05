package com.nulstudio.kwoocollector.net.model

import kotlinx.serialization.Serializable

@Serializable
data class NulResult<T> (
    val code: Int,
    val message: String?,
    val result: T?
)