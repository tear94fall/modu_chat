package com.example.modumessenger.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.Adapter.SearchFriendsAdapter;
import com.example.modumessenger.Retrofit.CommonData;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.MemberDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private final static String version = "version";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getVersion();

        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // Retrofit function
    public void getVersion() {
        Call<CommonData> call = RetrofitClient.getCommonApiService().RequestCommonData("version");

        call.enqueue(new Callback<CommonData>() {
            @Override
            public void onResponse(@NonNull Call<CommonData> call, @NonNull Response<CommonData> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                try {
                    assert response.body() != null;
                    Log.d("앱 버전 가져오기 요청: ", response.body().toString());

                    CommonData commonData = response.body();

                    String key = commonData.getKey();
                    String value = commonData.getValue();

                    if(key.equals(version)) {
                        // start app
                        Toast.makeText(getApplicationContext(), "버전 : " + value, Toast.LENGTH_SHORT).show();
                    } else {
                        // not start app
                    }

                } catch (Exception e) {
                    Log.e("오류 발생 : ", e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<CommonData> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}