package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.*;
import static com.google.android.gms.auth.api.signin.GoogleSignIn.*;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.R;
import com.example.modumessenger.dto.GoogleLoginRequest;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.dto.RequestLoginDto;
import com.example.modumessenger.dto.SignUpDto;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
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
    SignInButton LoginButton;
    ActivityResultLauncher<Intent> startActivityResult;
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

            LoginMember(new RequestLoginDto(account.getId(), account.getEmail()));
        }
    }

    private void bindingView() {
        setTitle("로그인");
        LoginButton = findViewById(R.id.googleButton);
        TextView textView = (TextView)LoginButton.getChildAt(0);
        textView.setText("Google 계정으로 로그인");
    }

    private void setLauncher() {
        startActivityResult = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d(TAG, "구글 소셜 로그인 성공. 백엔드에 회원가입 요청.");

                        Intent intent = result.getData();
                        Task<GoogleSignInAccount> task = getSignedInAccountFromIntent(intent);

                        SignupToBackend(task, "google");
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
    }

    private void setButtonClickEvent() {
        LoginButton.setOnClickListener(v -> {
            Log.d("로그인 버튼 클릭: ", "구글");
            Toast.makeText(getApplicationContext(),"회원 가입을 시작합니다.", Toast.LENGTH_SHORT).show();
            signIn();
        });
    }

    private void SignupToBackend(Task<GoogleSignInAccount> completedTask, String auth_type) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            SignupMember(new GoogleLoginRequest(account.getIdToken(), auth_type));
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityResult.launch(signInIntent);
    }

    // Retrofit function
    public void SignupMember(GoogleLoginRequest googleLoginRequest) {
        Call<SignUpDto> call = retrofitMemberAPI.RequestSignup(googleLoginRequest);

        call.enqueue(new Callback<SignUpDto>() {
            @Override
            public void onResponse(@NonNull Call<SignUpDto> call, @NonNull Response<SignUpDto> response) {
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        SignUpDto signUpDto = response.body();

                        Log.d("회원가입 완료 : ", signUpDto.toString());
                        LoginButton.setVisibility(View.INVISIBLE);

                        LoginMember(new RequestLoginDto(signUpDto.getUserId(), signUpDto.getEmail()));
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<SignUpDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void LoginMember(RequestLoginDto requestLoginDto){
        Call<Void> call = retrofitAuthAPI.login(requestLoginDto);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if(response.isSuccessful()) {
                    setDataStoreObject("refresh-token", "Bearer" + " " + response.headers().get("refresh-token"));
                    setDataStoreObject("access-token", "Bearer" + " " + response.headers().get("access-token"));

                    Log.e("refresh jwt 토큰 발급 완료 : ", getDataStoreStr("refresh-token"));
                    Log.e("access jwt 토큰 발급 완료 : ", getDataStoreStr("access-token"));

                    Toast.makeText(getApplicationContext(),"로그인에 성공 하였습니다. 반갑습니다.", Toast.LENGTH_SHORT).show();

                    retrofitMemberAPI = RetrofitClient.createMemberApiService(); // recreate with token at interceptor

                    GetUserInfo(requestLoginDto.getEmail(), "google");
                } else {
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    Toast.makeText(getApplicationContext(),"연결이 원활하지 않습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
                Toast.makeText(getApplicationContext(),"로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show();
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
