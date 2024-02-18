package com.example.modumessenger.Retrofit;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface RetrofitPushAPI {

    @PUT("push-service/push/{userId}/token")
    Call<String> RequestFcmToken(@Path("userId") String userId, @Body String fcmToken);
}
