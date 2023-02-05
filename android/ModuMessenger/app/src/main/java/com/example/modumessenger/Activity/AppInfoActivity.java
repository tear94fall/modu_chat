package com.example.modumessenger.Activity;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.Global.AppInfoUtil;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitCommonDataAPI;
import com.example.modumessenger.entity.CommonData;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AppInfoActivity extends AppCompatActivity {

    private final static String version = "version";

    TextView serverVersionTextView, appVersionTextView, osVersionTextView;

    String serverVersion, appVersion, osVersion;
    RetrofitCommonDataAPI retrofitCommonDataAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);

        bindingView();
        getData();
        setData();
        setButtonClickEvent();
    }

    private void bindingView() {
        setTitle("버전 정보");
        serverVersionTextView = findViewById(R.id.serverVersionTextView);
        appVersionTextView = findViewById(R.id.appVersionTextView);
        osVersionTextView = findViewById(R.id.osVersionTextView);
    }

    private void getData() {
        getVersion();
        appVersion = AppInfoUtil.getVersion(this);
        osVersion = Build.VERSION.RELEASE;
    }

    private void setData() {
        String accessToken = PreferenceManager.getString("access-token");
        retrofitCommonDataAPI = RetrofitClient.createCommonApiService(accessToken);

        if(serverVersion != null && !serverVersion.equals("")) { serverVersionTextView.setText("서버 버전 : " + serverVersion); }
        if(appVersion != null && !appVersion.equals("")) { appVersionTextView.setText("앱 버전 : " + appVersion); }
        if(osVersion != null && !osVersion.equals("")) { osVersionTextView.setText("os 버전 : 안드로이드 " + osVersion); }
    }

    private void setButtonClickEvent() {
    }

    // Retrofit function
    public void getVersion() {
        Call<CommonData> call = retrofitCommonDataAPI.RequestCommonData("version");

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

                    if(key.equals(version) && value != null && !value.equals("")) {
                        serverVersion = value;
                        serverVersionTextView.setText("서버 버전 : " + serverVersion);
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
