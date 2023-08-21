package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.GlideUtil.setProfileImage;
import static com.example.modumessenger.entity.ProfileType.*;

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

public class ProfileHistoryAdapter extends RecyclerView.Adapter<ProfileHistoryAdapter.ProfileHistoryViewHolder> {

    Member member;
    List<Profile> profileList;

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
    public ProfileHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.profile_image_history_row, parent, false);
        return new ProfileHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileHistoryViewHolder holder, int position) {
        Profile profile = profileList.get(position);

        holder.setProfileInfo(member, profile);
        holder.setClickEvent(profile);
        holder.setPopupMenuEvent(profile, listener);
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    public static class ProfileHistoryViewHolder extends RecyclerView.ViewHolder {

        ImageView profileImage, profileHistoryImage, profileMenu;
        TextView profileName, profileDate, profileStatusMessage;

        public ProfileHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            profileName = itemView.findViewById(R.id.profile_history_my_profile_name);
            profileDate = itemView.findViewById(R.id.profile_history_my_profile_date);
            profileStatusMessage = itemView.findViewById(R.id.profile_history_status_message);

            profileImage = itemView.findViewById(R.id.profile_history_my_profile_image);
            profileHistoryImage = itemView.findViewById(R.id.profile_history_image);
            profileMenu = itemView.findViewById(R.id.profile_menu_button);
        }

        public void setProfileInfo(Member member, Profile profile) {
            if(profile.getProfileType() == PROFILE_IMAGE || profile.getProfileType() == PROFILE_WALLPAPER) {
                setProfileHistoryImage(profile.getValue());
                profileStatusMessage.setVisibility(View.GONE);
            } else if (profile.getProfileType() == PROFILE_STATUS_MESSAGE) {
                setProfileStatusMessage(profile.getValue());
                profileHistoryImage.setVisibility(View.GONE);
            }

            setUserProfileImage(member.getProfileImage());
            setProfileName(member.getUsername(), profile.getProfileType());
            setProfileDate(profile.getCreatedDateTime());
        }

        public void setClickEvent(Profile profile) {
            profileImage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("memberId", String.valueOf(profile.getMemberId()));

                v.getContext().startActivity(intent);
            });

            profileHistoryImage.setOnClickListener(v -> {
                // need to implementation
            });
        }

        public void setPopupMenuEvent(Profile profile, ProfileMenuClickListener listener) {
            profileMenu.setOnClickListener(v -> listener.onItemLongClick(v, profile));
        }

        public void setUserProfileImage(String imageUrl) {
            setProfileImage(profileImage, imageUrl);
        }

        public void setProfileHistoryImage(String imageUrl) {
            setProfileImage(profileHistoryImage, imageUrl);
        }

        public void setProfileStatusMessage(String statusMessage) {
            profileStatusMessage.setText(statusMessage);

            float fontSize = statusMessage.length() < 15 ? 25 : 20;
            profileStatusMessage.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize);
        }

        public void setProfileName(String name, ProfileType type) {
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

            profileName.setText(msg);
        }

        public void setProfileDate(String updateDate) {
            String []date = updateDate.substring(0, updateDate.indexOf(" ")).split("-");
            String result = String.format("%s년 %s월 %s일", date[0], date[1], date[2]);

            profileDate.setText(result);
        }
    }
}
