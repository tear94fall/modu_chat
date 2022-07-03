package com.example.modumessenger.Activity;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.NotificationAdapter;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.entity.CommonData;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity {

    List<CommonData> notifyList;
    RecyclerView notificationRecyclerView;
    NotificationAdapter notificationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        bindingView();
        getData();
        setData();
        setButtonClickEvent();
    }

    private void bindingView() {
        setTitle("공지사항");

        notificationRecyclerView = findViewById(R.id.notification_recycler_view);
        notificationRecyclerView.setHasFixedSize(true);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationRecyclerView.scrollToPosition(0);
    }

    private void getData() {
        getNotification();
    }

    private void setData() {
        notifyList = new ArrayList<>();
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

                    notifyList = response.body();
                    notificationAdapter = new NotificationAdapter(notifyList);
                    notificationRecyclerView.setAdapter(notificationAdapter);

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
