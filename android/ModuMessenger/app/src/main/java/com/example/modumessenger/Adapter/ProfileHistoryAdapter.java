package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.GlideUtil.setProfileImage;
import static com.example.modumessenger.entity.ProfileType.*;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.R;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.entity.Profile;
import com.example.modumessenger.entity.ProfileType;

import java.util.ArrayList;
import java.util.List;

public class ProfileHistoryAdapter extends RecyclerView.Adapter<ProfileHistoryAdapter.ProfileHistoryViewHolder> {

    Member member;
    List<Profile> profileList;

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
        holder.setDataAtIndex(member, profileList, position);
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    public static class ProfileHistoryViewHolder extends RecyclerView.ViewHolder {

        ImageView profileImage, profileHistoryImage;
        TextView profileName, profileDate;

        public ProfileHistoryViewHolder(@NonNull View itemView) {
            super(itemView);

            profileName = itemView.findViewById(R.id.profile_history_my_profile_name);
            profileDate = itemView.findViewById(R.id.profile_history_my_profile_date);

            profileImage = itemView.findViewById(R.id.profile_history_my_profile_image);
            profileHistoryImage = itemView.findViewById(R.id.profile_history_image);
        }

        public void setDataAtIndex(Member member, List<Profile> profileList, int position) {
            Profile profile = profileList.get(position);
            if(profile.getProfileType() == PROFILE_IMAGE || profile.getProfileType() == PROFILE_WALLPAPER) {
                setProfileHistoryImage(profile.getValue());
            }
            // add if type is status message

            setUserProfileImage(member.getProfileImage());
            setProfileName(member.getUsername(), profile.getProfileType());
            setProfileDate(profile.getCreatedDateTime());
        }

        public void setUserProfileImage(String imageUrl) {
            setProfileImage(profileImage, imageUrl);
        }

        public void setProfileHistoryImage(String imageUrl) {
            setProfileImage(profileHistoryImage, imageUrl);
        }

        public void setProfileName(String name, ProfileType type) {
            String msg = name;

            if (type == PROFILE_IMAGE) {
                msg += "님의 프로필 이미지";
            } else if (type == PROFILE_WALLPAPER) {
                msg += "님의 배경 이미지";
            }
            // add if type is status message

            profileName.setText(msg);
        }

        public void setProfileDate(String date) {
            profileDate.setText(date.substring(0, date.indexOf(" ")));
        }
    }
}
