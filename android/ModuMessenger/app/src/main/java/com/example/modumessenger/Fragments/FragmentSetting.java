package com.example.modumessenger.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;

public class FragmentSetting extends Fragment {

    String userId;
    ImageView favoriteImageView, BlockedImageView, ThemeImageView, BackupImageView;

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
        userId = PreferenceManager.getString("userId");
    }

    private void bindingView(View view) {
        favoriteImageView = view.findViewById(R.id.favorite_image_view);
        BlockedImageView = view.findViewById(R.id.blocked_image_view);
        ThemeImageView = view.findViewById(R.id.theme_image_view);
        BackupImageView = view.findViewById(R.id.backup_image_view);
    }

    private void setButtonClickEvent() {
        favoriteImageView.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "즐겨 찾기", Toast.LENGTH_SHORT).show();
        });

        BlockedImageView.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "차단 하기", Toast.LENGTH_SHORT).show();
        });

        ThemeImageView.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "테마 설정", Toast.LENGTH_SHORT).show();
        });

        BackupImageView.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "백업 하기", Toast.LENGTH_SHORT).show();
        });
    }
}