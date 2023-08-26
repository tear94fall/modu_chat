package com.example.modumessenger.Activity;

import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.R;

public class ChatImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_image);

        bindingView();
        setData();
        getData();
        setEvents();
    }

    private void bindingView() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
    }

    private void setData() {
    }

    private void getData() {
    }

    private void setEvents() {
    }
}
