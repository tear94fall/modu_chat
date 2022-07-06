package com.example.modumessenger.Activity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.modumessenger.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ProfileEditBottomSheetFragment extends BottomSheetDialogFragment {

    private ProfileEditBottomSheetListener mListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_edit_bottom_sheet, container, false);

        mListener = (ProfileEditBottomSheetListener) getContext();

        ConstraintLayout galleryConstraintLayout = view.findViewById(R.id.change_profile_image_gallery);
        galleryConstraintLayout.setOnClickListener(v -> {
            mListener.onButtonClicked(1);
            dismiss();
        });

        ConstraintLayout defaultConstraintLayout = view.findViewById(R.id.change_profile_image_default);
        defaultConstraintLayout.setOnClickListener(v -> {
            mListener.onButtonClicked(2);
            dismiss();
        });

        return view;
    }

    public interface ProfileEditBottomSheetListener {
        void onButtonClicked(int type);
    }
}
