package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.R;
import com.example.modumessenger.dto.MemberDto;

import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.MyViewHolder> {

    List<MemberDto> friendsList;

    public FriendsAdapter(List<MemberDto> friendsList) {
        this.friendsList = friendsList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.friend_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        MemberDto member = this.friendsList.get(position);

        holder.setUserInfo(member);
        holder.setUserClickEvent(member);
    }

    @Override
    public int getItemCount() {
        return this.friendsList.size();
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView username;
        TextView statusMessage;
        ImageView profileImage;
        ConstraintLayout cardViewLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.my_user_name);
            statusMessage = itemView.findViewById(R.id.my_status_message);
            profileImage = itemView.findViewById(R.id.my_profile_image);
            cardViewLayout = itemView.findViewById(R.id.cardViewLayout);
        }

        public void setUserInfo(MemberDto member) {
            this.username.setText(member.getUsername());
            this.statusMessage.setText(member.getStatusMessage());
            setProfileImage(profileImage, member.getProfileImage());
        }

        public void setUserClickEvent(MemberDto member) {
            this.cardViewLayout.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("memberId", String.valueOf(member.getId()));

                v.getContext().startActivity(intent);
            });
        }
    }
}
