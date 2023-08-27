package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.CreateRoomActivity;
import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.R;
import com.example.modumessenger.dto.MemberDto;

import java.util.List;

public class CreateRoomAdapter extends RecyclerView.Adapter<CreateRoomAdapter.AddChatViewHolder> {

    List<MemberDto> addFriendsList;

    public CreateRoomAdapter(List<MemberDto> addFriendsList) { this.addFriendsList = addFriendsList; }

    @NonNull
    @Override
    public CreateRoomAdapter.AddChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.friend_invite_row, parent, false);
        return new CreateRoomAdapter.AddChatViewHolder(parent.getContext(), view);
    }

    @Override
    public void onBindViewHolder(@NonNull CreateRoomAdapter.AddChatViewHolder holder, int position) {
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
        Context context;

        TextView username;
        TextView statusMessage;
        ImageView profileImage;
        CheckBox addChatCheck;
        ConstraintLayout addChatCardViewLayout;

        public AddChatViewHolder(Context context, @NonNull View itemView) {
            super(itemView);
            this.context = context;

            username = itemView.findViewById(R.id.add_user_name);
            statusMessage = itemView.findViewById(R.id.add_status_message);
            profileImage = itemView.findViewById(R.id.add_profile_image);
            addChatCheck = itemView.findViewById(R.id.invite_check_button);
            addChatCardViewLayout = itemView.findViewById(R.id.addChatCardViewLayout);
        }

        public void setUserInfo(MemberDto member) {
            this.username.setText(member.getUsername());
            this.statusMessage.setText(member.getStatusMessage());
            setProfileImage(profileImage, member.getProfileImage());
        }

        public void setUserClickEvent(MemberDto member) {
            this.addChatCardViewLayout.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("memberId", String.valueOf(member.getId()));

                v.getContext().startActivity(intent);
            });
        }

        public void setAddChatButton(MemberDto member) {
            this.addChatCheck.setOnClickListener(view -> {
                boolean checked = ((CheckBox) view).isChecked();

                if(checked) {
                    ((CreateRoomActivity) this.context).addUserIdOnAddChatList(member.getId());
                } else {
                    ((CreateRoomActivity) this.context).removeUserIdOnAddChatList(member.getId());
                }
            });
        }
    }
}

