package com.example.modumessenger.Fragments;

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

import com.bumptech.glide.Glide;
import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.Grid.SettingGridAdapter;
import com.example.modumessenger.Grid.SettingGridItem;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.MemberDto;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentSetting extends Fragment {

    MemberDto myInfo;
    ConstraintLayout setting_my_profile_card_view;
    ImageView myProfileImage;
    TextView myProfileName, myProfileEmail;

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

        getMyProfileInfo(new MemberDto(PreferenceManager.getString("userId"), PreferenceManager.getString("email")));
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
            intent.putExtra("userId", myInfo.getUserId());
            intent.putExtra("username", myInfo.getUsername());
            intent.putExtra("statusMessage", myInfo.getStatusMessage());
            intent.putExtra("profileImage", myInfo.getProfileImage());

            view.getContext().startActivity(intent);
        });
    }

    // Retrofit function
    public void getMyProfileInfo(MemberDto memberDto) {
        Call<MemberDto> call = RetrofitClient.getMemberApiService().RequestUserId(memberDto);

        call.enqueue(new Callback<MemberDto>() {
            @Override
            public void onResponse(@NonNull Call<MemberDto> call, @NonNull Response<MemberDto> response) {
                if(!response.isSuccessful()){
                    Log.e("연결이 비정상적 : ", "error code : " + response.code());
                    return;
                }

                assert response.body() != null;
                myInfo = response.body();

                // get my Profile Info
                myProfileName.setText(myInfo.getUsername());
                myProfileEmail.setText(myInfo.getEmail());
                Glide.with(requireContext())
                        .load(myInfo.getProfileImage())
                        .error(Glide.with(requireContext())
                                .load(R.drawable.basic_profile_image)
                                .into(myProfileImage))
                        .into(myProfileImage);

                if(memberDto.getEmail().equals(myInfo.getEmail())){
                    Log.d("중복검사: ", "중복된 번호가 아닙니다");
                }

                Log.d("내 정보 가져오기 요청 : ", response.body().toString());
            }

            @Override
            public void onFailure(@NonNull Call<MemberDto> call, @NonNull Throwable t) {
                Log.e("연결실패", t.getMessage());
            }
        });
    }
}