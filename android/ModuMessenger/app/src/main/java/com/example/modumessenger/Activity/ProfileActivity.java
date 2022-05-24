package com.example.modumessenger.Activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Global.OnSwipeListener;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;

import java.util.Objects;

public class ProfileActivity extends AppCompatActivity implements View.OnTouchListener{

    ImageView profileImageView;
    TextView usernameTextView;
    TextView statusMessageTextView;
    Button profileEditButton;
    Button profileCloseButton;

    String username;
    String statusMessage;
    String profileImage;

    GestureDetector gestureDetector;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        ActionBar actionBar = getSupportActionBar();
        Objects.requireNonNull(actionBar).hide();

        gestureDetector = new GestureDetector(this,new OnSwipeListener(){
            @Override
            public boolean onSwipe(Direction direction) {
                if (direction==Direction.up){
                    //do your stuff
                    Log.d("Swipe Up", "onSwipe: up");
                }

                if (direction==Direction.down){
                    //do your stuff
                    Log.d("Swipe Down", "onSwipe: down");
                    finish();
                }
                return true;
            }
        });

        profileImageView = findViewById(R.id.profile_activity_image);
        profileImageView.setOnTouchListener(this);

        usernameTextView = findViewById(R.id.profile_activity_username);
        statusMessageTextView = findViewById(R.id.profile_activity_status_message);

        profileEditButton = findViewById(R.id.profile_edit_button);
        profileEditButton.setVisibility(View.INVISIBLE);
        profileEditButton.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProfileEditActivity.class);

            intent.putExtra("username", PreferenceManager.getString("username"));
            intent.putExtra("statusMessage", PreferenceManager.getString("statusMessage"));
            intent.putExtra("profileImage", PreferenceManager.getString("profileImage"));

            startActivity(intent);
        });

        profileCloseButton = findViewById(R.id.profile_close_button);

        profileCloseButton.setOnClickListener(view -> finish());

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

        if(username!=null && username.length() !=0 && username.equals(PreferenceManager.getString("username"))) {
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.d(event.toString(), "onTouch: ");
        gestureDetector.onTouchEvent(event);
        return true;
    }
}