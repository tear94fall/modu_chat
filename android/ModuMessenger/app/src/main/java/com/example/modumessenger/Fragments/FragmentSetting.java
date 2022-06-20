package com.example.modumessenger.Fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.Grid.SettingGridAdapter;
import com.example.modumessenger.R;

public class FragmentSetting extends Fragment {

    String userId;

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
        GridView settingGridView = view.findViewById(R.id.grid_test);
        SettingGridAdapter settingGridAdapter = new SettingGridAdapter(requireActivity());
        settingGridView.setAdapter(settingGridAdapter);

        settingGridAdapter.setGridItems();

        settingGridView.setOnItemClickListener((parent, view1, position, id) -> {
            String itemName = settingGridAdapter.getGridItem(position).getItemName();
            Toast.makeText(requireActivity().getApplicationContext(), itemName, Toast.LENGTH_SHORT).show();
        });
    }

    private void setButtonClickEvent() {
    }
}