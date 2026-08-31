package com.example.modumessenger.Activity;

import static com.example.modumessenger.Global.DataStoreHelper.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.modumessenger.Fragments.FragmentFriends;
import com.example.modumessenger.Fragments.FragmentChat;
import com.example.modumessenger.Fragments.FragmentSetting;
import com.example.modumessenger.Global.App;
import com.example.modumessenger.Global.ChatBanner;
import com.example.modumessenger.Global.UiUtil;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitChatAPI;
import com.example.modumessenger.Retrofit.RetrofitChatRoomAPI;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitPushAPI;
import com.example.modumessenger.dto.ChatRoomDto;
import com.example.modumessenger.entity.Member;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    Member member;

    BottomNavigationView bottomNavigationView;
    BadgeDrawable badgeDrawable;
    private ViewPager2 viewPager2;

    RetrofitPushAPI retrofitPushAPI;
    RetrofitChatRoomAPI retrofitChatRoomAPI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        App.getChatRepository().getBanner().observe(this,
                event -> ChatBanner.show(this, event));

        getData();
        setData();
        bindingView();
        initFirebase();
        setButtonClickEvent();
    }

    private void getData() {
        member = getDataStoreMember();
    }

    private void setData() {
        retrofitPushAPI = RetrofitClient.createPushApiService();
        retrofitChatRoomAPI = RetrofitClient.createChatRoomApiService();
    }

    private void initFirebase() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                setDataStoreObject("fcm-token", task.getResult());
                SendFcmToken(member.getUserId(), getDataStoreStr("fcm-token"));
                getChatRoomList(member.getId());
            } else {
                System.out.println("fcm get token error");
            }
        });
    }

    private void bindingView() {
        setTitle("친구");

        bottomNavigationView = findViewById(R.id.navigationView);
        viewPager2 = findViewById(R.id.view_pager);
        viewPager2.setAdapter(new ViewPagerAdapter(this));

        badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.chatItem);

        badgeDrawable.setVerticalOffset(UiUtil.DpToPx(MainActivity.this, 4));
        badgeDrawable.setHorizontalOffset(UiUtil.DpToPx(MainActivity.this, 1));
        // 999 를 넘으면 "999+" 로 접힌다. 방 목록 배지와 같은 규칙이다.
        badgeDrawable.setMaxCharacterCount(4);

        badgeDrawable.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.badge_red));
        badgeDrawable.setBadgeTextColor(ContextCompat.getColor(MainActivity.this, R.color.white));

        badgeDrawable.setVisible(false);

        // 방별 배지와 같은 출처를 쓴다. 서버 병합·실시간 증가·읽음 소거가 모두 여기로 흘러온다.
        App.getChatRepository().getTotalUnreadCount().observe(this, this::setChatTabBadge);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 하단 탭 배지는 어느 탭에 있든 최신이어야 한다. 방 목록은 채팅 탭 프래그먼트가
        // 실제로 만들어질 때 비로소 채워지므로, 친구/설정 탭에서 시작하면 합계가 0 으로 남는다.
        // 소켓의 onReconnected 는 '재'연결에만 오므로 최초 실행을 덮지 못한다.
        App.getChatRepository().refreshChatRooms();
    }

    private void setChatTabBadge(Integer total) {
        if (total == null || total <= 0) {
            badgeDrawable.setVisible(false);
            return;
        }

        badgeDrawable.setNumber(total);
        badgeDrawable.setVisible(true);
    }

    private void setButtonClickEvent() {
        bottomNavigationView.setOnItemSelectedListener(new ItemSelectedListener());

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }

            @Override
            public void onPageSelected(int position) {
                switch (position) {
                    case 0:
                        setTitle("친구");
                        bottomNavigationView.getMenu().findItem(R.id.friendsItem).setChecked(true);
                        break;
                    case 1:
                        setTitle("채팅");
                        bottomNavigationView.getMenu().findItem(R.id.chatItem).setChecked(true);
                        break;
                    case 2:
                        setTitle("설정");
                        bottomNavigationView.getMenu().findItem(R.id.settingItem).setChecked(true);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    class ItemSelectedListener implements NavigationBarView.OnItemSelectedListener {
        @SuppressLint("NonConstantResourceId")
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();

            if (itemId == R.id.friendsItem) {
                setTitle("친구");
                viewPager2.setCurrentItem(0);
            } else if (itemId == R.id.chatItem) {
                setTitle("채팅");
                viewPager2.setCurrentItem(1);
            } else if (itemId == R.id.settingItem) {
                setTitle("설정");
                viewPager2.setCurrentItem(2);
            }

            return true;
        }
    }

    static class ViewPagerAdapter extends FragmentStateAdapter {
        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = new FragmentFriends();
                    break;
                case 1:
                    fragment = new FragmentChat();
                    break;
                case 2:
                    fragment = new FragmentSetting();
                    break;
            }

            assert fragment != null;
            return fragment;
        }

        @Override
        public int getItemCount() {
            return 3;
        }
    }

    // Retrofit function
    public void SendFcmToken(String userId, String token) {
        Call<String> call = retrofitPushAPI.RequestFcmToken(userId, token);

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        if(!response.body().equals(token)) {
                            Toast.makeText(getApplicationContext(), "메시지 알림 서버와의 연결이 불안정합니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                Log.d("채팅방 fcm 토큰 전송 요청 : ", token);
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }

    public void getChatRoomList(Long memberId) {
        Call<List<ChatRoomDto>> call = retrofitChatRoomAPI.RequestChatRooms(Long.toString(memberId));

        call.enqueue(new Callback<List<ChatRoomDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatRoomDto>> call, @NonNull Response<List<ChatRoomDto>> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        List<ChatRoomDto> chatRoomDtoList = response.body();
                        chatRoomDtoList.forEach(chatRoomDto -> FirebaseMessaging.getInstance().subscribeToTopic(chatRoomDto.getRoomId()));
                    }
                }

                Log.d("채팅방 리스트 가져오기 요청 : ", Long.toString(memberId));
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatRoomDto>> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}