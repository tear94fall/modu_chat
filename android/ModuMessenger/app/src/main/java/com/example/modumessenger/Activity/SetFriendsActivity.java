package com.example.modumessenger.Activity;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.SetFriendsAdapter;
import com.example.modumessenger.R;

public class SetFriendsActivity extends AppCompatActivity {

    RecyclerView findFriendRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_friends);

        bindingView();
        getData();
        setData();
        setButtonClickEvent();
        settingSideNavBar();
    }

    private void bindingView() {
        setTitle("친구 설정");

        findFriendRecyclerView = findViewById(R.id.set_friends_recycler_view);
        findFriendRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        findFriendRecyclerView.setHasFixedSize(true);
        findFriendRecyclerView.scrollToPosition(0);
    }

    private void getData() {
    }

    private void setData() {
        findFriendRecyclerView.setAdapter(new SetFriendsAdapter());
    }

    private void setButtonClickEvent() {
    }

    private void settingSideNavBar() {
    }
}
