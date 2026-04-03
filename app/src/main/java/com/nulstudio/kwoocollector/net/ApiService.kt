package com.nulstudio.kwoocollector.net

import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {
    @POST("/v1/verify")
    fun verifyToken(): Response<Unit>
}