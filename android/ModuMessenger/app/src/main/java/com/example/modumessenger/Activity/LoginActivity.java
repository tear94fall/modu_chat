package com.example.modumessenger.Activity;

import static com.google.android.gms.auth.api.signin.GoogleSignIn.*;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.Global.App;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.example.modumessenger.Retrofit.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "Oauth2Google";

    GoogleSignInClient mGoogleSignInClient;

    SignInButton LoginButton;

    ActivityResultLauncher<Intent> startActivityResult = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d(TAG, "구글 소셜 로그인 성공. 백엔드에 회원가입 요청.");

                    Intent intent = result.getData();
                    Task<GoogleSignInAccount> task = getSignedInAccountFromIntent(intent);

                    SignupToBackend(task);
                }
            });

    // 백엔드에 회원 가입 요청
    private void SignupToBackend(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            Member member = new Member(account.getEmail(), account.getEmail());
            member.setAuth("google");
            member.setUsername(account.getDisplayName());
            member.setStatusMessage("Hello! Modu Chat!");
            member.setProfileImage(account.getPhotoUrl().toString());

            SignupMember(member);
        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setTitle("Modu Login");

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = getClient(this, gso);

        LoginButton = findViewById(R.id.googleButton);

        Button.OnClickListener onClickListener = v -> {
            if (v.getId() == R.id.googleButton) {
                Log.d("로그인 버튼 클릭: ", "구글");
                Toast.makeText(this.getApplicationContext(),"회원 가입을 시작합니다.", Toast.LENGTH_SHORT).show();
                signIn();
            }
        };

        LoginButton.setOnClickListener(onClickListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = getLastSignedInAccount(this);

        if(account!=null){
            Log.d("이미 가입된 사용자 입니다. 소셜 로그인 시도: ", "구글");
            Toast.makeText(this.getApplicationContext(),"로그인을 시도 합니다.", Toast.LENGTH_SHORT).show();

            LoginButton.setVisibility(View.INVISIBLE);
            GetUserIdByLogin(account.getEmail());
        }
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityResult.launch(signInIntent);
    }

    public void SignupMember(Member member) {
        Call<Member> call = RetrofitClient.getApiService().RequestSignup(member);

        call.enqueue(new Callback<Member>() {
            @Override
            public void onResponse(@NonNull Call<Member> call, @NonNull Response<Member> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                Member result = response.body();

                assert response.body() != null;
                assert result != null;

                if(member.getEmail().equals(result.getEmail())){
                    Log.d("중복검사: ", "중복된 번호가 아닙니다");
                }

                Log.d("회원가입 요청 : ", response.body().toString());
                LoginMember(result.getUserId(), result.getEmail());
            }

            @Override
            public void onFailure(@NonNull Call<Member> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void LoginMember(String userId, String email){
        Member member = new Member(userId, email);
        Call<Void> call = RetrofitClient.getApiService().RequestLogin(member);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void > response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    Toast.makeText(getApplicationContext(),"연결이 원활하지 않습니다.", Toast.LENGTH_SHORT).show();
                }

                String jwtToken = response.headers().get("token");
                Log.e("jwt 토큰 발급 완료 : ", jwtToken);
                PreferenceManager.setString("token", "Bearer" + " " + jwtToken);

                Toast.makeText(getApplicationContext(),"로그인에 성공 하였습니다. 반갑습니다.", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
                Toast.makeText(getApplicationContext(),"로그인에 실패하였습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void GetUserIdByLogin(String email) {
        Member member = new Member(email);
        Call<Member> call = RetrofitClient.getApiService().RequestUserId(member);

        call.enqueue(new Callback<Member>() {
            @Override
            public void onResponse(@NonNull Call<Member> call, @NonNull Response<Member> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    Toast.makeText(getApplicationContext(),"연결이 원활하지 않습니다.", Toast.LENGTH_SHORT).show();
                }

                Member result = response.body();
                assert result != null;

                LoginMember(result.getUserId(), result.getEmail());
            }

            @Override
            public void onFailure(@NonNull Call<Member> call, @NonNull Throwable t) {
                Log.e("사용자 아이디 가져오기 실패", t.getMessage());
            }
        });
    }
}
