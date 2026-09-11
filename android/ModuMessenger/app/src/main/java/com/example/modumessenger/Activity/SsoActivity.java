package com.example.modumessenger.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.Global.DataStoreHelper;
import com.example.modumessenger.Global.SsoRequest;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitAuthAPI;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.SsoCodeRequestDto;
import com.example.modumessenger.dto.SsoCodeResponseDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 다른 모두 앱(커머스)이 "채팅 앱에 로그인된 계정으로 나도 로그인" 을 요청하면, 사용자에게 물어본 뒤
 * auth-service 에서 1회용 SSO 코드를 받아 돌려준다. 같은 서명 키의 앱만 부를 수 있다(manifest 의 signature 권한).
 * 액세스 토큰 자체는 절대 넘기지 않는다.
 */
public class SsoActivity extends AppCompatActivity {

    private static final String TAG = "SsoActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!SsoRequest.isAllowedCaller(getCallingPackage())) {
            cancel("caller_not_allowed");
            return;
        }
        Intent intent = getIntent();
        SsoRequest request = SsoRequest.from(intent.getStringExtra("client_id"),
                intent.getStringExtra("code_challenge"), intent.getStringExtra("code_challenge_method"));
        if (request == null) {
            cancel("bad_request");
            return;
        }
        if (!Boolean.TRUE.equals(DataStoreHelper.checkDataStoreKey("access-token"))
                || !Boolean.TRUE.equals(DataStoreHelper.checkDataStoreKey("member"))) {
            cancel("not_logged_in");
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Modu_Dialog)
                .setTitle("모두 계정으로 로그인")
                .setMessage("모두의 커머스가 내 모두 계정으로 로그인하려 합니다. 허용할까요?")
                .setNegativeButton("거부", (d, w) -> cancel("denied"))
                .setPositiveButton("허용", (d, w) -> issueCode(request))
                .setOnCancelListener(d -> cancel("denied"))
                .create();
        dialog.show();
    }

    private void issueCode(SsoRequest request) {
        RetrofitAuthAPI api = RetrofitClient.createAuthApiService();
        api.ssoCode(new SsoCodeRequestDto(request.getClientId(), request.getCodeChallenge(), request.getCodeChallengeMethod()))
                .enqueue(new Callback<SsoCodeResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<SsoCodeResponseDto> call, @NonNull Response<SsoCodeResponseDto> response) {
                        if (!response.isSuccessful() || response.body() == null || response.body().getCode() == null) {
                            Log.e(TAG, "sso code 발급 실패 code=" + response.code());
                            cancel("server_error");
                            return;
                        }
                        Intent result = new Intent();
                        result.putExtra("code", response.body().getCode());
                        setResult(Activity.RESULT_OK, result);
                        finish();
                    }

                    @Override
                    public void onFailure(@NonNull Call<SsoCodeResponseDto> call, @NonNull Throwable t) {
                        Log.e(TAG, "sso code 발급 실패: " + t.getMessage());
                        cancel("server_error");
                    }
                });
    }

    private void cancel(String reason) {
        Intent result = new Intent();
        result.putExtra("reason", reason);
        setResult(Activity.RESULT_CANCELED, result);
        finish();
    }
}
