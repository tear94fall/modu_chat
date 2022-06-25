package com.example.modumessenger.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.modumessenger.R;

public class SetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        bindingView();
        getData();
        setData();
        setButtonClickEvent();
    }

    private void bindingView() {
        setTitle("설정");
    }

    private void getData() {
    }

    private void setData() {
    }

    private void setButtonClickEvent() {
    }
}
