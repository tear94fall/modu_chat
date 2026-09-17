package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.*;
import static com.google.android.gms.auth.api.signin.GoogleSignIn.*;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.Global.App;
import com.example.modumessenger.Global.DataStoreHelper;
import com.example.modumessenger.R;
import com.example.modumessenger.Global.OAuthClient;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.dto.TokenResponseDto;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.example.modumessenger.Retrofit.*;
import com.google.gson.Gson;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "Oauth2Google";

    GoogleSignInClient mGoogleSignInClient;
    MaterialButton LoginButton;
    ActivityResultLauncher<Intent> startActivityResult;
    Handler handler;
    RetrofitMemberAPI retrofitMemberAPI;
    RetrofitAuthAPI retrofitAuthAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        bindingView();
        setLauncher();
        getData();
        setData();
        setButtonClickEvent();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = getLastSignedInAccount(this);

        if(account!=null){
            Log.d("이미 가입된 사용자 입니다. 소셜 로그인 시도: ", "구글");
            Toast.makeText(this.getApplicationContext(),"로그인을 시도 합니다.", Toast.LENGTH_SHORT).show();

            LoginButton.setVisibility(View.INVISIBLE);

            // 저장된 계정으로 새 구글 ID 토큰을 받아 교환한다. 서버는 구글 서명으로만 신원을 믿는다.
            mGoogleSignInClient.silentSignIn().addOnCompleteListener(this, task -> {
                if (task.isSuccessful() && task.getResult() != null && task.getResult().getIdToken() != null) {
                    exchangeGoogleIdToken(task.getResult());
                } else {
                    Log.w(TAG, "구글 무음 로그인 실패, 버튼으로 다시 로그인");
                    LoginButton.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        handler.removeCallbacksAndMessages(null);
    }

    private void bindingView() {
        setTitle("로그인");
        LoginButton = findViewById(R.id.googleButton);
    }

    private void setLauncher() {
        startActivityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "구글 소셜 로그인 성공. auth-service 에 토큰 교환 요청.");

                        Intent intent = result.getData();
                        Task<GoogleSignInAccount> task = getSignedInAccountFromIntent(intent);

                        onGoogleSignedIn(task);
                    }
                });
    }

    private void getData() {
    }

    private void setData() {
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
        retrofitAuthAPI = RetrofitClient.createAuthApiService();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = getClient(this, gso);

        handler = new Handler();
    }

    private void setButtonClickEvent() {
        LoginButton.setOnClickListener(v -> {
            Log.d("로그인 버튼 클릭: ", "구글");
            Toast.makeText(getApplicationContext(),"회원 가입을 시작합니다.", Toast.LENGTH_SHORT).show();
            signIn();
        });
    }

    private void onGoogleSignedIn(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            exchangeGoogleIdToken(account);
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(getApplicationContext(),
                    String.format("구글 로그인에 실패했습니다 (코드 %d)", e.getStatusCode()), Toast.LENGTH_SHORT).show();
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityResult.launch(signInIntent);
    }

    /**
     * 구글 ID 토큰을 auth-service 토큰으로 바꾼다. 가입은 서버가 첫 로그인 때 알아서 한다.
     * 토큰은 예전 헤더 방식과 같은 키에 "Bearer " 를 붙여 저장해 나머지 화면은 그대로 동작한다.
     */
    private void exchangeGoogleIdToken(GoogleSignInAccount account) {
        String idToken = account.getIdToken();
        if (idToken == null) {
            Toast.makeText(getApplicationContext(), "구글 ID 토큰을 받지 못했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            LoginButton.setVisibility(View.VISIBLE);
            return;
        }
        LoginButton.setVisibility(View.INVISIBLE);

        Call<TokenResponseDto> call = retrofitAuthAPI.token(OAuthClient.googleForm(idToken));
        call.enqueue(new Callback<TokenResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<TokenResponseDto> call, @NonNull Response<TokenResponseDto> response) {
                if (!response.isSuccessful() || response.body() == null || response.body().getAccessToken() == null) {
                    Log.e(TAG, "토큰 교환 실패 code=" + response.code());
                    Toast.makeText(getApplicationContext(),
                            String.format("로그인에 실패했습니다 (코드 %d)", response.code()), Toast.LENGTH_SHORT).show();
                    LoginButton.setVisibility(View.VISIBLE);
                    mGoogleSignInClient.signOut();
                    return;
                }
                TokenResponseDto tokens = response.body();
                setDataStoreObject("access-token", "Bearer " + tokens.getAccessToken());
                setDataStoreObject("refresh-token", "Bearer " + tokens.getRefreshToken());

                Toast.makeText(getApplicationContext(), "로그인에 성공 하였습니다. 반갑습니다.", Toast.LENGTH_SHORT).show();

                retrofitMemberAPI = RetrofitClient.createMemberApiService(); // recreate with token at interceptor
                GetUserInfo(account.getEmail(), "google");
            }

            @Override
            public void onFailure(@NonNull Call<TokenResponseDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
                Toast.makeText(getApplicationContext(), "로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                LoginButton.setVisibility(View.VISIBLE);
            }
        });
    }

    public void GetUserInfo(String email, String auth_type) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUserInfo(email);

        APIHelper.enqueueWithRetry(call, 5, new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(response.isSuccessful()) {
                    MemberDto result = response.body();

                    if(result != null) {
                        String member = new Gson().toJson(result);
                        setDataStoreObject("member", member);
                        App.onLoggedIn(result.getUserId(), String.valueOf(result.getId()));

                        Log.d("내정보 가져오기 요청 : ", result.toString());

                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    Toast.makeText(getApplicationContext(),"연결이 원활하지 않습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("사용자 아이디 가져오기 실패", t.getMessage());

                Toast.makeText(getApplicationContext(),"오프라인 모드 입니다.", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
