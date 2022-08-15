package com.example.modumessenger.Activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.modumessenger.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class ProfileEditBottomSheetFragment extends BottomSheetDialogFragment {

    private ProfileEditBottomSheetListener mListener;
    private final int image_type;

    public ProfileEditBottomSheetFragment(int image_type) {
        this.image_type = image_type;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_edit_bottom_sheet, container, false);

        if(this.image_type == PROFILE_IMAGE_TYPE.PROFILE_WALLPAPER.getType()) {
            TextView textView = view.findViewById(R.id.change_profile_banner_text);
            textView.setText("배경화면 설정");
        }

        mListener = (ProfileEditBottomSheetListener) getContext();

        ConstraintLayout galleryConstraintLayout = view.findViewById(R.id.change_profile_image_gallery);
        galleryConstraintLayout.setOnClickListener(v -> {
            mListener.onButtonClicked(this.image_type == PROFILE_IMAGE_TYPE.PROFILE_IMAGE.getType() ? PROFILE_EVENT.PROFILE_CHANGE.getEvent() : PROFILE_EVENT.WALLPAPER_CHANGE.getEvent());
            dismiss();
        });

        ConstraintLayout defaultConstraintLayout = view.findViewById(R.id.change_profile_image_default);
        defaultConstraintLayout.setOnClickListener(v -> {
            mListener.onButtonClicked(this.image_type == PROFILE_IMAGE_TYPE.PROFILE_IMAGE.getType() ? PROFILE_EVENT.PROFILE_DEFAULT.getEvent() : PROFILE_EVENT.WALLPAPER_DEFAULT.getEvent());
            dismiss();
        });

        return view;
    }

    public interface ProfileEditBottomSheetListener {
        void onButtonClicked(int event);
    }

    public enum PROFILE_IMAGE_TYPE {
        PROFILE_IMAGE(0),
        PROFILE_WALLPAPER(1);

        private final int type;
        private PROFILE_IMAGE_TYPE(int type) {
            this.type = type;
        }

        public int getType() {
            return this.type;
        }
    }

    public enum PROFILE_EVENT {
        PROFILE_CHANGE(0),
        PROFILE_DEFAULT(1),
        WALLPAPER_CHANGE(2),
        WALLPAPER_DEFAULT(3);

        private final int event;
        private PROFILE_EVENT(int event) {
            this.event = event;
        }

        public int getEvent() {
            return this.event;
        }
    }
}
