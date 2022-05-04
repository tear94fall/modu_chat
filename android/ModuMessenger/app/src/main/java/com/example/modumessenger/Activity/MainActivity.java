package com.example.modumessenger.Activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Toast;

import com.example.modumessenger.Fragments.FragmentFriends;
import com.example.modumessenger.Fragments.FragmentChat;
import com.example.modumessenger.Fragments.FragmentSetting;
import com.example.modumessenger.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private final FragmentManager fragmentManager = getSupportFragmentManager();
    private final FragmentFriends fragmentFriends = new FragmentFriends();
    private final FragmentChat fragmentChat = new FragmentChat();
    private final FragmentSetting fragmentSetting = new FragmentSetting();

    private boolean isReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(() -> {
            for(int i=0;i<5;i++){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            isReady = true;
        }).start();

        final View content = findViewById(android.R.id.content);
        content.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        if (isReady) {
                            content.getViewTreeObserver().removeOnPreDrawListener(this);
                            return true;
                        } else {
                            return false;
                        }
                    }
                });

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.frameLayout, fragmentFriends).commitAllowingStateLoss();
        setTitle("친구");

        BottomNavigationView bottomNavigationView = findViewById(R.id.navigationView);
        bottomNavigationView.setOnItemSelectedListener(new ItemSelectedListener());
    }

    class ItemSelectedListener implements NavigationBarView.OnItemSelectedListener{
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            FragmentTransaction transaction = fragmentManager.beginTransaction();

            switch(menuItem.getItemId()) {
                case R.id.friendsItem:
                    setTitle("친구");
                    transaction.replace(R.id.frameLayout, fragmentFriends).commitAllowingStateLoss();
                    break;
                case R.id.chatItem:
                    setTitle("채팅");
                    transaction.replace(R.id.frameLayout, fragmentChat).commitAllowingStateLoss();
                    break;
                case R.id.settingItem:
                    setTitle("설정");
                    transaction.replace(R.id.frameLayout, fragmentSetting).commitAllowingStateLoss();
                    break;
            }
            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_friends_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if(itemId == R.id.menu_search) {
            Toast.makeText(this, "검색", Toast.LENGTH_LONG).show();
        } else if(itemId == R.id.menu_settings) {
            Toast.makeText(this, "설정", Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }
}