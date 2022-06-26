package com.example.modumessenger.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.SetUpAdapter;
import com.example.modumessenger.R;

public class SetupActivity extends AppCompatActivity {

    RecyclerView setUpRecyclerView;

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

        setUpRecyclerView = findViewById(R.id.set_up_recycler_view);
        setUpRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        setUpRecyclerView.setHasFixedSize(true);
        setUpRecyclerView.scrollToPosition(0);
    }

    private void getData() {
    }

    private void setData() {
        setUpRecyclerView.setAdapter(new SetUpAdapter());
    }

    private void setButtonClickEvent() {
    }
}
