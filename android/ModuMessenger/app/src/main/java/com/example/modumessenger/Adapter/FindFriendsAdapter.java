package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;
import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.R;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.Member;

import java.util.List;

public class FindFriendsAdapter extends RecyclerView.Adapter<FindFriendsAdapter.FindFriendsViewHolder> {

    List<MemberDto> findFriendsList;
    Member member;

    public FindFriendsAdapter(List<MemberDto> findFriendsList) { this.findFriendsList = findFriendsList; }

    @NonNull
    @Override
    public FindFriendsAdapter.FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.friend_find_row, parent, false);
        return new FindFriendsAdapter.FindFriendsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FindFriendsAdapter.FindFriendsViewHolder holder, int position) {
        MemberDto memberDto = this.findFriendsList.get(position);

        holder.setUserInfo(memberDto);
        holder.setUserClickEvent(memberDto);
        holder.setAddFriendsButton(memberDto);

        member = getDataStoreMember();

        findFriendsList.forEach(m -> {
            if(member.getUserId().equals(memberDto.getUserId())) {
                holder.chatFriendsButton.setVisibility(View.INVISIBLE);
            }
        });
    }

    @Override
    public int getItemCount() {
        return findFriendsList.size();
    }

    public static class FindFriendsViewHolder extends RecyclerView.ViewHolder {
        TextView username, statusMessage;
        ImageView profileImage;
        Button chatFriendsButton;
        ConstraintLayout cardViewLayout;

        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.find_user_name);
            statusMessage = itemView.findViewById(R.id.find_status_message);
            profileImage = itemView.findViewById(R.id.find_profile_image);
            chatFriendsButton = itemView.findViewById(R.id.chat_friends_button);
            cardViewLayout = itemView.findViewById(R.id.findFriendCardViewLayout);
        }

        public void setUserInfo(MemberDto member) {
            this.username.setText(member.getUsername());
            this.statusMessage.setText(member.getStatusMessage());
            setProfileImage(profileImage, member.getProfileImage());
        }

        public void setUserClickEvent(MemberDto member) {
            this.cardViewLayout.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("email", member.getEmail());
                intent.putExtra("userId", member.getUserId());

                v.getContext().startActivity(intent);
            });
        }

        public void setAddFriendsButton(MemberDto member) {
            this.chatFriendsButton.setOnClickListener(view -> {
                // if exist chat room, go to exist
                // if not exit chat room, create chat room
            });
        }
    }
}