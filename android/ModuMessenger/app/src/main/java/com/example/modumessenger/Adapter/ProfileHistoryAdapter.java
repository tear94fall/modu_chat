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

import java.util.List;

public class ProfileHistoryAdapter extends RecyclerView.Adapter<ProfileHistoryAdapter.FindFriendsViewHolder> {

    Member member;
    List<Profile> profileList;

    public ProfileHistoryAdapter(Member member) {
        this.member = member;
        this.profileList = member.getAllProfileListDesc();
    }

    @NonNull
    @Override
    public ProfileHistoryAdapter.FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.profile_image_history_row, parent, false);
        return new ProfileHistoryAdapter.FindFriendsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProfileHistoryAdapter.FindFriendsViewHolder holder, int position) {
        holder.setDataAtIndex(member, profileList, position);
    }

    @Override
    public int getItemCount() {
        return profileList.size();
    }

    public static class FindFriendsViewHolder extends RecyclerView.ViewHolder {

        ImageView profileImage, profileHistoryImage;
        TextView profileName;

        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            profileName = itemView.findViewById(R.id.profile_history_my_profile_name);

            profileImage = itemView.findViewById(R.id.profile_history_my_profile_image);
            profileHistoryImage = itemView.findViewById(R.id.profile_history_image);
        }

        public void setDataAtIndex(Member member, List<Profile> profileList, int position) {
            setUserProfileImage(member.getProfileImage());
            setProfileName(member.getUsername());

            Profile profile = profileList.get(position);
            if(profile.getProfileType() == PROFILE_IMAGE || profile.getProfileType() == PROFILE_WALLPAPER) {
                setProfileHistoryImage(profile.getValue());
            }
        }

        public void setUserProfileImage(String imageUrl) {
            setProfileImage(profileImage, imageUrl);
        }

        public void setProfileHistoryImage(String imageUrl) {
            setProfileImage(profileHistoryImage, imageUrl);
        }

        public void setProfileName(String name) {
            profileName.setText(name);
        }
    }
}
