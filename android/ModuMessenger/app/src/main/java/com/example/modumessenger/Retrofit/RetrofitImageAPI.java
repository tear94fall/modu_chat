package com.example.modumessenger.Retrofit;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface RetrofitImageAPI {

    @Multipart
    @POST("chat-service/image")
    Call<String> RequestUploadImage(@Part MultipartBody.Part file);
}
