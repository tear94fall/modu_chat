package com.example.modumessenger.Activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class SetAccountActivity extends AppCompatActivity {

    GoogleSignInOptions googleSignInOptions;
    GoogleSignInClient googleSignInClient;

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
    }

    private void setButtonClickEvent() {
        Button logoutButton = findViewById(R.id.logout_button);

        logoutButton.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("로그아웃").setMessage("로그아웃 하시겠습니까?")
                .setPositiveButton("로그아웃", (dialog, whichButton) -> {
                    Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    googleSignInClient.signOut()
                            .addOnCompleteListener(this, task -> {
                                startActivity(intent);
                                finish();
                            });
                })
                .setNegativeButton("취소", (dialog, whichButton) -> {

                })
                .show());
    }

    private void settingSideNavBar() {
    }
}
