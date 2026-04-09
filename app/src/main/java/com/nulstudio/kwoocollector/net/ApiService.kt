package com.nulstudio.kwoocollector.net

import com.nulstudio.kwoocollector.net.model.NulResult
import com.nulstudio.kwoocollector.net.model.request.ChangePasswordRequest
import com.nulstudio.kwoocollector.net.model.request.LoginRequest
import com.nulstudio.kwoocollector.net.model.request.RegisterRequest
import com.nulstudio.kwoocollector.net.model.response.EntryOverallResponse
import com.nulstudio.kwoocollector.net.model.response.FormAbstractResponse
import com.nulstudio.kwoocollector.net.model.response.FormDetailResponse
import com.nulstudio.kwoocollector.net.model.response.FormHistoryDetailResponse
import com.nulstudio.kwoocollector.net.model.response.ProfileResponse
import com.nulstudio.kwoocollector.net.model.response.TableAbstractResponse
import com.nulstudio.kwoocollector.net.model.response.UpdateResponse
import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("/v1/verify")
    suspend fun verify(): NulResult<Unit>

    @POST("/v1/login")
    suspend fun login(@Body request: LoginRequest): NulResult<String>

    @POST("/v1/register")
    suspend fun register(@Body request: RegisterRequest): NulResult<String>

    @POST("/v1/logout")
    suspend fun logout(): NulResult<Unit>

    @POST("/v1/profile/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): NulResult<Unit>

    @GET("/v1/profile")
    suspend fun fetchProfile(): NulResult<ProfileResponse>

    @GET("/v1/update")
    suspend fun fetchUpdates(): NulResult<UpdateResponse>

    @GET("/v1/forms")
    suspend fun fetchForms(): NulResult<List<FormAbstractResponse>>

    @GET("/v1/forms/history")
    suspend fun fetchFormHistory(): NulResult<List<FormAbstractResponse>>

    @GET("/v1/forms/history/{formId}")
    suspend fun fetchFormHistoryDetail(@Path("formId") formId: Int): NulResult<FormHistoryDetailResponse>

    @GET("/v1/forms/{formId}")
    suspend fun fetchForm(@Path("formId") formId: Int): NulResult<FormDetailResponse>

    @POST("/v1/forms/{formId}")
    suspend fun fillForm(@Path("formId") formId: Int, @Body content: JsonObject): NulResult<Unit>

    @Multipart
    @POST("/v1/media/upload")
    suspend fun uploadImage(@Part file: MultipartBody.Part): NulResult<String>

    @GET("/v1/tables")
    suspend fun fetchTables(): NulResult<List<TableAbstractResponse>>

    @GET("/v1/tables/{tableId}")
    suspend fun listTable(
        @Path("tableId") tableId: Int,
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("keyword") keyword: String = ""
    ): NulResult<EntryOverallResponse>

    @GET("/v1/tables/{tableId}/{entryId}")
    suspend fun fetchEntry(
        @Path("tableId") tableId: Int,
        @Path("entryId") entryId: Int
    ): NulResult<JsonObject>

    @POST("/v1/tables/{tableId}")
    suspend fun createEntry(@Path("tableId") tableId: Int, @Body content: JsonObject): NulResult<Unit>

    @PUT("/v1/tables/{tableId}/{entryId}")
    suspend fun updateEntry(
        @Path("tableId") tableId: Int,
        @Path("entryId") entryId: Int,
        @Body content: JsonObject
    ): NulResult<Unit>

    @DELETE("/v1/tables/{tableId}/{entryId}")
    suspend fun deleteEntry(@Path("tableId") tableId: Int, @Path("entryId") entryId: Int): NulResult<Unit>
}
