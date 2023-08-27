package com.example.modumessenger.Fragments;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;
import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.Grid.SettingGridAdapter;
import com.example.modumessenger.Grid.SettingGridItem;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.Retrofit.RetrofitMemberAPI;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.Member;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentSetting extends Fragment {

    Member member;

    ConstraintLayout setting_my_profile_card_view;
    ImageView myProfileImage;
    TextView myProfileName, myProfileEmail;
    RetrofitMemberAPI retrofitMemberAPI;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("DEBUG", "onCreate of FragmentSetting");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setData();
        bindingView(view);
        setButtonClickEvent();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.e("DEBUG", "onResume of FragmentSetting");

        getMyProfileInfo(member.getEmail());
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.e("DEBUG", "onPause of FragmentSetting");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.e("DEBUG", "onStop of FragmentSetting");
    }

    private void setData() {
        member = getDataStoreMember();
        retrofitMemberAPI = RetrofitClient.createMemberApiService();
    }

    private void bindingView(View view) {
        myProfileImage = view.findViewById(R.id.setting_my_profile_image);
        myProfileName = view.findViewById(R.id.setting_my_profile_name);
        myProfileEmail = view.findViewById(R.id.setting_my_profile_email);

        setting_my_profile_card_view = view.findViewById(R.id.setting_my_profile_card_view);

        GridView settingGridView = view.findViewById(R.id.setting_grid);
        SettingGridAdapter settingGridAdapter = new SettingGridAdapter(requireActivity());
        settingGridView.setAdapter(settingGridAdapter);

        settingGridAdapter.setGridItems(view);

        settingGridView.setOnItemClickListener((parent, view1, position, id) -> {
            SettingGridItem gridItem = settingGridAdapter.getGridItem(position);
            Toast.makeText(requireActivity().getApplicationContext(), gridItem.getItemName(), Toast.LENGTH_SHORT).show();

            Intent intent = gridItem.getIntent();
            if(intent != null) {
                view.getContext().startActivity(intent);
            }
        });
    }

    private void setButtonClickEvent() {
        setting_my_profile_card_view.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), ProfileActivity.class);
            intent.putExtra("memberId", String.valueOf(member.getId()));

            view.getContext().startActivity(intent);
        });
    }

    private void setUserProfile(Member member) {
        myProfileName.setText(member.getUsername());
        myProfileEmail.setText(member.getEmail());

        setProfileImage(myProfileImage, member.getProfileImage());
    }

    // Retrofit function
    public void getMyProfileInfo(String email) {
        Call<MemberDto> call = retrofitMemberAPI.RequestUserInfo(email);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(response.isSuccessful()) {
                    if(response.body() != null) {
                        MemberDto memberDto = response.body();
                        member.updateProfile(memberDto);
                        setUserProfile(member);

                        Log.d("내 정보 가져 오기 요청 : ", response.body().toString());
                    }
                }
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}