package com.example.modumessenger.Activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.R;

public class ProfileActivity extends AppCompatActivity {

    ImageView profileImageView;
    TextView usernameTextView;
    TextView statusMessageTextView;

    String username;
    String statusMessage;
    int profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_profile);

        profileImageView = findViewById(R.id.profile_activity_image);
        usernameTextView = findViewById(R.id.profile_activity_username);
        statusMessageTextView = findViewById(R.id.profile_activity_status_message);

        getData();
        setData();
    }
    private void getData() {
        if(getIntentExtra("images") && getIntentExtra("title") && getIntentExtra("description"))
        {
            username = getIntent().getStringExtra("title");
            statusMessage = getIntent().getStringExtra("description");
            profileImage = getIntent().getIntExtra("images",1);
        } else {
            Toast.makeText(this, "No Data", Toast.LENGTH_SHORT).show();
        }
    }

    private void setData() {
        setTextOnView(usernameTextView, username);
        setTextOnView(statusMessageTextView, statusMessage);
        setImageOnView(profileImageView, profileImage);
    }

    private boolean getIntentExtra(String key) {
        return getIntent().hasExtra("images");
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