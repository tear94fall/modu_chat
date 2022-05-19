package com.example.modumessenger.Adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Activity.InviteActivity;
import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.R;
import com.example.modumessenger.dto.MemberDto;

import java.util.List;

public class inviteAdapter extends RecyclerView.Adapter<inviteAdapter.AddChatViewHolder> {

    List<MemberDto> addFriendsList;

    public inviteAdapter(List<MemberDto> addFriendsList) { this.addFriendsList = addFriendsList; }

    @NonNull
    @Override
    public inviteAdapter.AddChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.friend_invite_row, parent, false);
        return new inviteAdapter.AddChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull inviteAdapter.AddChatViewHolder holder, int position) {
        MemberDto member = this.addFriendsList.get(position);

        holder.setUserInfo(member);
        holder.setUserClickEvent(member);
        holder.setAddChatButton(member);
    }

    @Override
    public int getItemCount() {
        return addFriendsList.size();
    }

    public static class AddChatViewHolder extends RecyclerView.ViewHolder {
        InviteActivity addChatActivity;

        TextView username;
        TextView statusMessage;
        ImageView profileImage;
        CheckBox addCHatCheck;
        ConstraintLayout addChatCardViewLayout;

        public AddChatViewHolder(@NonNull View itemView) {
            super(itemView);
            addChatActivity = InviteActivity.getInstance();

            username = itemView.findViewById(R.id.add_user_name);
            statusMessage = itemView.findViewById(R.id.add_status_message);
            profileImage = itemView.findViewById(R.id.add_profile_image);
            addCHatCheck = itemView.findViewById(R.id.invite_check_button);
            addChatCardViewLayout = itemView.findViewById(R.id.addChatCardViewLayout);
        }

        public void setUserInfo(MemberDto member) {
            this.username.setText(member.getUsername());
            this.statusMessage.setText(member.getStatusMessage());
            Glide.with(profileImage)
                    .load(member.getProfileImage())
                    .error(Glide.with(profileImage)
                            .load(R.drawable.basic_profile_image)
                            .into(profileImage))
                    .into(profileImage);
        }

        public void setUserClickEvent(MemberDto member) {
            this.addChatCardViewLayout.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("username", member.getUsername());
                intent.putExtra("statusMessage", member.getStatusMessage());
                intent.putExtra("profileImage", member.getProfileImage());

                v.getContext().startActivity(intent);
            });
        }

        public void setAddChatButton(MemberDto member) {
            this.addCHatCheck.setOnClickListener(view -> {
                boolean checked = ((CheckBox) view).isChecked();

                if(checked) {
                    addChatActivity.addUserIdOnAddChatList(member.getUserId());
                } else {
                    addChatActivity.removeUserIdOnAddChatList(member.getUserId());
                }
            });
        }
    }
}

