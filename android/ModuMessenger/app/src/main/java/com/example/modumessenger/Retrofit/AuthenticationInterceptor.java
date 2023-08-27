package com.example.modumessenger.Retrofit;

import androidx.annotation.NonNull;

import com.example.modumessenger.Global.DataStoreHelper;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthenticationInterceptor implements Interceptor {

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();

        Request.Builder builder;

        if (DataStoreHelper.checkDataStoreKey("access-token")) {
            String accessToken = DataStoreHelper.getDataStoreStr("access-token");

            builder = original.newBuilder()
                    .header("Authorization", accessToken);
        } else {
            builder = original.newBuilder();
        }

        Request request = builder.build();
        return chain.proceed(request);
    }
}
