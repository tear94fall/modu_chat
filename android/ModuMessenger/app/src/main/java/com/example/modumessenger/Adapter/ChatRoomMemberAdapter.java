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
import com.example.modumessenger.entity.Member;

import java.util.List;

public class ChatRoomMemberAdapter extends RecyclerView.Adapter<ChatRoomMemberAdapter.ChatRoomMemberHolder> {

    List<Member> chatRoomMemberList;

    public ChatRoomMemberAdapter(List<Member> memberList) {
        this.chatRoomMemberList = memberList;
    }

    @NonNull
    @Override
    public ChatRoomMemberAdapter.ChatRoomMemberHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.chat_room_member_row, parent, false);
        return new ChatRoomMemberAdapter.ChatRoomMemberHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomMemberAdapter.ChatRoomMemberHolder holder, int position) {
        Member member = this.chatRoomMemberList.get(position);

        holder.setUserInfo(member);
        holder.setUserClickEvent(member);
    }

    @Override
    public int getItemCount() {
        return this.chatRoomMemberList.size();
    }

    public static class ChatRoomMemberHolder extends RecyclerView.ViewHolder {

        TextView username;
        TextView statusMessage;
        ImageView profileImage;
        ConstraintLayout cardViewLayout;

        public ChatRoomMemberHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.chat_room_member_name);
            statusMessage = itemView.findViewById(R.id.chat_room_member_status_message);
            profileImage = itemView.findViewById(R.id.chat_room_member_image);
            cardViewLayout = itemView.findViewById(R.id.chat_room_member_card_view);
        }

        public void setUserInfo(Member member) {
            this.username.setText(member.getUsername());

            setStatusMessage(member.getStatusMessage());
            setProfileImage(profileImage, member.getProfileImage());
        }

        public void setStatusMessage(String statusMessage) {
            String msg = statusMessage.length() > 15 ? statusMessage.substring(0, 12) + "..." : statusMessage;
            this.statusMessage.setText(msg);
        }

        public void setUserClickEvent(Member member) {
            this.cardViewLayout.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("memberId", String.valueOf(member.getId()));
                intent.putExtra("email", member.getEmail());
                intent.putExtra("userId", member.getUserId());
                intent.putExtra("username", member.getUsername());
                intent.putExtra("statusMessage", member.getStatusMessage());
                intent.putExtra("profileImage", member.getProfileImage());
                intent.putExtra("wallpaperImage", member.getWallpaperImage());

                v.getContext().startActivity(intent);
            });
        }
    }
}
