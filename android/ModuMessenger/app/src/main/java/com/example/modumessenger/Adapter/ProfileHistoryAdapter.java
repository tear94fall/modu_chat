package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

import android.content.Intent;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.Activity.ProfileImageActivity;
import com.example.modumessenger.R;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.entity.Profile;
import com.example.modumessenger.entity.ProfileType;

import java.util.ArrayList;
import java.util.List;

public class ProfileHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    Member member;
    List<Profile> profileList;

    int INVALID = -1;
    int TEXT = 0;
    int IMAGE = 1;
    int WALLPAPER = 2;

    private ProfileMenuClickListener listener;

    public interface ProfileMenuClickListener {
        void onItemLongClick(View view, Profile profile);
    }

    public void setProfileMenuClickListener(ProfileMenuClickListener listener) {
        this.listener = listener;
    }

    public ProfileHistoryAdapter(Member member) {
        this.member = member;
        this.profileList = member.getProfiles() != null ? member.getAllProfileListDesc() : new ArrayList<>();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if(viewType == TEXT) {
            viewHolder = new TextProfileHistoryViewHolder(inflater.inflate(R.layout.profile_text_history_row, parent, false));
        } else if(viewType == IMAGE || viewType == WALLPAPER) {
            viewHolder = new ImageProfileHistoryViewHolder(inflater.inflate(R.layout.profile_image_history_row, parent, false));
        } else {
            viewHolder = new TextProfileHistoryViewHolder(inflater.inflate(R.layout.profile_text_history_row, parent, false));
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Profile profile = profileList.get(position);

        if (holder instanceof ImageProfileHistoryViewHolder) {
            ImageProfileHistoryViewHolder viewHolder = ((ImageProfileHistoryViewHolder) holder);
            viewHolder.setProfileInfo(member, profile);
            viewHolder.setClickEvent(profile, listener);
        } else if (holder instanceof TextProfileHistoryViewHolder) {
            TextProfileHistoryViewHolder viewHolder = ((TextProfileHistoryViewHolder) holder);
            viewHolder.setProfileInfo(member, profile);
            viewHolder.setClickEvent(profile, listener);
        }
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    @Override
    public int getItemViewType(int position) {
        switch (profileList.get(position).getProfileType()) {
            case PROFILE_STATUS_MESSAGE:
                return TEXT;
            case PROFILE_IMAGE:
                return IMAGE;
            case PROFILE_WALLPAPER:
                return WALLPAPER;
            default:
                return INVALID;
        }
    }

    public static class ImageProfileHistoryViewHolder extends RecyclerView.ViewHolder {

        ImageView profileImage, profileHistoryImage, profileMenu;
        TextView profileName, profileDate;

        public ImageProfileHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            profileName = itemView.findViewById(R.id.profile_history_my_profile_name);
            profileDate = itemView.findViewById(R.id.profile_history_my_profile_date);
            profileImage = itemView.findViewById(R.id.profile_history_my_profile_image);
            profileMenu = itemView.findViewById(R.id.profile_menu_button);
            profileHistoryImage = itemView.findViewById(R.id.profile_history_image);
        }

        public void setProfileInfo(Member member, Profile profile) {
            setProfileImage(profileHistoryImage, profile.getValue());
            setProfileImage(profileImage, member.getProfileImage());

            profileName.setText(getFullProfileName(member.getUsername(), profile.getProfileType()));
            profileDate.setText(getFullProfileDate(profile.getLastModifiedDate()));
        }

        public void setClickEvent(Profile profile, ProfileMenuClickListener listener) {
            profileImage.setOnClickListener(v -> openProfileActivityIntent(v, profile.getMemberId()));
            profileHistoryImage.setOnClickListener(v -> openProfileImageActivityIntent(v, profile));
            profileMenu.setOnClickListener(v -> listener.onItemLongClick(v, profile));
        }
    }

    public static class TextProfileHistoryViewHolder extends RecyclerView.ViewHolder {

        ImageView profileImage, profileMenu;
        TextView profileName, profileDate, profileStatusMessage;

        public TextProfileHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            profileName = itemView.findViewById(R.id.profile_history_my_profile_name);
            profileDate = itemView.findViewById(R.id.profile_history_my_profile_date);
            profileImage = itemView.findViewById(R.id.profile_history_my_profile_image);
            profileMenu = itemView.findViewById(R.id.profile_menu_button);
            profileStatusMessage = itemView.findViewById(R.id.profile_history_status_message);
        }

        public void setProfileInfo(Member member, Profile profile) {
            setProfileImage(profileImage, member.getProfileImage());

            profileName.setText(getFullProfileName(member.getUsername(), profile.getProfileType()));
            profileDate.setText(getFullProfileDate(profile.getLastModifiedDate()));

            profileStatusMessage.setText(profile.getValue());
            profileStatusMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, profile.getValue().length() < 15 ? 25 : 20);
        }

        public void setClickEvent(Profile profile, ProfileMenuClickListener listener) {
            profileImage.setOnClickListener(v -> openProfileActivityIntent(v, profile.getMemberId()));
            profileMenu.setOnClickListener(v -> listener.onItemLongClick(v, profile));
        }
    }

    public static String getFullProfileName(String name, ProfileType type) {
        String msg = name;

        switch(type) {
            case PROFILE_IMAGE:
                msg += "님의 프로필 이미지";
                break;
            case PROFILE_WALLPAPER:
                msg += "님의 배경 이미지";
                break;
            case PROFILE_STATUS_MESSAGE:
                msg += "님의 상태 메세지";
                break;
            default:
                break;
        }

        return msg;
    }

    public static String getFullProfileDate(String dateTime) {
        String []date = dateTime.substring(0, dateTime.indexOf(" ")).split("-");
        return String.format("%s년 %s월 %s일", date[0], date[1], date[2]);
    }

    public static void openProfileActivityIntent(View view, Long memberId) {
        Intent intent = new Intent(view.getContext(), ProfileActivity.class);
        intent.putExtra("memberId", String.valueOf(memberId));

        view.getContext().startActivity(intent);
    }

    public static void openProfileImageActivityIntent(View view, Profile profile) {
        Intent intent = new Intent(view.getContext(), ProfileImageActivity.class);
        intent.putExtra("memberId", String.valueOf(profile.getMemberId()));
        intent.putExtra("profileId", String.valueOf(profile.getId()));
        intent.putExtra("type", profile.getProfileType().name());

        view.getContext().startActivity(intent);
    }
}
