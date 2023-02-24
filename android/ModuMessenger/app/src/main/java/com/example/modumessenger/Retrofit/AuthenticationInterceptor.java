package com.example.modumessenger.Retrofit;

import androidx.annotation.NonNull;

import com.example.modumessenger.Global.PreferenceManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthenticationInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        String accessToken = PreferenceManager.getString("access-token");

        Request original = chain.request();

        Request.Builder builder = original.newBuilder()
                .header("Authorization", accessToken);

        Request request = builder.build();
        return chain.proceed(request);
    }
}
