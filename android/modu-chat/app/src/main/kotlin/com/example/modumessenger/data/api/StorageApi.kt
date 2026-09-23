package com.example.modumessenger.data.api

import com.example.modumessenger.data.dto.FileInfoDto
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import retrofit2.http.Streaming

interface StorageApi {

    /**
     * 파트 이름은 반드시 `file`. 응답 본문은 저장된 파일 이름 문자열이라 [ResponseBody] 로 받아 직접 읽는다
     * (ScalarsConverter 를 쓰지 않는다).
     */
    @Multipart
    @POST("storage-service/api-public/upload")
    suspend fun upload(@Part file: MultipartBody.Part): ResponseBody

    /** 원본 이름·크기·종류. 파일 말풍선이 쓴다. */
    @GET("storage-service/api-public/file-info")
    suspend fun fileInfo(@Query("file") name: String): FileInfoDto

    /** 파일 본문. 큰 파일도 메모리에 다 올리지 않고 흘려 받는다. */
    @Streaming
    @GET("storage-service/api-public/download")
    suspend fun download(@Query("file") name: String): ResponseBody
}
