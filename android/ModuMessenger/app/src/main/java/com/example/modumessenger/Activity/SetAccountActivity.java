package com.example.modumessenger.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.Global.App;
import com.example.modumessenger.Global.DataStoreHelper;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitAuthAPI;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SetAccountActivity extends AppCompatActivity {

    GoogleSignInOptions googleSignInOptions;
    GoogleSignInClient googleSignInClient;
    RetrofitAuthAPI retrofitAuthAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_account);

        bindingView();
        getData();
        setData();
        setButtonClickEvent();
        settingSideNavBar();
    }

    private void bindingView() {
        setTitle("계정 설정");
    }

    private void getData() {
        googleSignInOptions = new GoogleSignInOptions
                .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);
    }

    private void setData() {
        retrofitAuthAPI = RetrofitClient.createAuthApiService();
    }

    private void setButtonClickEvent() {
        Button logoutButton = findViewById(R.id.logout_button);

        logoutButton.setOnClickListener(v -> {
            AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_Modu_Dialog)
                    .setTitle("로그아웃")
                    .setMessage("로그아웃 하시겠습니까?")
                    .setNegativeButton("취소", (d, which) -> d.dismiss())
                    .setPositiveButton("로그아웃", (d, which) -> RequestLogout())
                    .create();

            dialog.show();

            // 나가기 팝업과 같이, 되돌릴 수 없는 쪽만 붉게 둔다.
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(getResources().getColor(R.color.red, getTheme()));
        });
    }

    private void settingSideNavBar() {
    }

    private void logoutGoogle(Intent intent) {
        if(intent != null) {
            googleSignInClient.signOut()
                    .addOnCompleteListener(this, task -> {
                        App.onLoggedOut();
                        DataStoreHelper.clearDataStore();

                        startActivity(intent);
                        finish();
                    });
        }
    }

    // Retrofit function
    public void RequestLogout() {
        Call<Void> call = retrofitAuthAPI.logout();

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if(response.isSuccessful()) {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    logoutGoogle(intent);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}
