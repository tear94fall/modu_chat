package com.example.modumessenger.Activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;

import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    ImageView profileImageView;
    TextView usernameTextView;
    TextView statusMessageTextView;
    Button profileEditButton;
    Button profileCloseButton;

    String username;
    String statusMessage;
    String profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).hide();

        profileImageView = findViewById(R.id.profile_activity_image);
        usernameTextView = findViewById(R.id.profile_activity_username);
        statusMessageTextView = findViewById(R.id.profile_activity_status_message);

        profileEditButton = findViewById(R.id.profile_edit_button);
        profileEditButton.setVisibility(View.INVISIBLE);

        profileCloseButton = findViewById(R.id.profile_close_button);

        profileCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        getData();
        setData();
    }

    private void getData() {
        if(getIntentExtra("profileImage") && getIntentExtra("username") && getIntentExtra("statusMessage"))
        {
            username = getIntent().getStringExtra("username");
            statusMessage = getIntent().getStringExtra("statusMessage");
            profileImage = getIntent().getStringExtra("profileImage");
        } else {
            Toast.makeText(this, "No Data", Toast.LENGTH_SHORT).show();
        }
    }

    private void setData() {
        setTextOnView(usernameTextView, username);
        setTextOnView(statusMessageTextView, statusMessage);
        Glide.with(this).load(profileImage).into(profileImageView);

        if(username.equals(PreferenceManager.getString("username"))) {
            profileEditButton.setVisibility(View.VISIBLE);
        }
    }

    private boolean getIntentExtra(String key) {
        return getIntent().hasExtra(key);
    }

    private void setTextOnView(TextView view, String value) {
        if(value != null && !value.equals("")) {
            view.setText(value);
        } else {
            view.setText("No Value");
        }
    }

    private void setImageOnView(ImageView view, int value) {
        view.setImageResource(value);
    }
}