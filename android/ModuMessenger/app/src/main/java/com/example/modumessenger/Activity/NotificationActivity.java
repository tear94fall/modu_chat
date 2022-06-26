package com.example.modumessenger.Activity;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.entity.CommonData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {

    Map<String, String> notifyMap = new ConcurrentHashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        bindingView();
        getData();
        setData();
        setButtonClickEvent();
    }

    private void bindingView() {
        setTitle("공지사항");
    }

    private void getData() {
        getNotification();
    }

    private void setData() {
    }

    private void setButtonClickEvent() {
    }

    // Retrofit function
    public void getNotification() {
        Call<List<CommonData>> call = RetrofitClient.getCommonApiService().RequestCommonDataList("notification");

        call.enqueue(new Callback<List<CommonData>>() {
            @Override
            public void onResponse(@NonNull Call<List<CommonData>> call, @NonNull Response<List<CommonData>> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                try {
                    assert response.body() != null;
                    Log.d("공지 사항 가져오기 요청: ", response.body().toString());

                    List<CommonData> commonDataList = response.body();

                    commonDataList.forEach(column -> {
                        notifyMap.put(column.getKey(), column.getValue());
                    });

                } catch (Exception e) {
                    Log.e("오류 발생 : ", e.getMessage());
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<CommonData>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}
