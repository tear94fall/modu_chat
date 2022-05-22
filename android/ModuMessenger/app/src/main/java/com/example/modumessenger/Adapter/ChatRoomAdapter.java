package com.example.modumessenger.Adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Activity.ChatActivity;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.ChatRoom;
import com.example.modumessenger.dto.ChatRoomDto;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {

    List<ChatRoom> chatRoomList;

    public ChatRoomAdapter(List<ChatRoom> chatRoomList) {
        this.chatRoomList = (chatRoomList == null || chatRoomList.size() == 0) ? new ArrayList<>() : chatRoomList;
    }

    @NonNull
    @Override
    public ChatRoomAdapter.ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.chat_room_row, parent, false);
        return new ChatRoomAdapter.ChatRoomViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomAdapter.ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = this.chatRoomList.get(position);

        holder.setChatRoomInfo(chatRoom);
        holder.setChatRoomClickEvent(chatRoom);
    }

    @Override
    public int getItemCount() {
        return this.chatRoomList.size();
    }

    public static class ChatRoomViewHolder extends RecyclerView.ViewHolder {

        TextView chatRoomName;
        TextView lastChatMessage;
        TextView lastChatTime;
        ImageView chatRoomImage;
        ConstraintLayout chatRoomCardView;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            chatRoomName = itemView.findViewById(R.id.chat_room_name);
            lastChatMessage = itemView.findViewById(R.id.last_chat_message);
            lastChatTime = itemView.findViewById(R.id.last_chat_time);
            chatRoomImage = itemView.findViewById(R.id.chat_room_image);
            chatRoomCardView = itemView.findViewById(R.id.chatRoomCardViewLayout);
        }

        public void setChatRoomInfo(ChatRoom chatRoom) {
            this.chatRoomName.setText(chatRoom.getRoomName());
            this.lastChatMessage.setText(chatRoom.getLastChatMsg());
            this.lastChatTime.setText(chatRoom.getLastChatTime().toString());

            if(chatRoom.getRoomImage()!=null && !chatRoom.getRoomImage().equals("")) {
                Glide.with(chatRoomImage)
                        .load(chatRoom.getRoomImage())
                        .error(Glide.with(chatRoomImage)
                                .load(R.drawable.basic_profile_image)
                                .into(chatRoomImage))
                        .into(chatRoomImage);
            } else {
                Glide.with(chatRoomImage)
                        .load(R.drawable.basic_profile_image)
                        .into(chatRoomImage);
            }
        }

        public void setChatRoomClickEvent(ChatRoom chatRoom) {
            this.chatRoomCardView.setOnClickListener(view -> {
                Intent intent = new Intent(view.getContext(), ChatActivity.class);
                intent.putExtra("roomId", chatRoom.getRoomId());
                view.getContext().startActivity(intent);
            });
        }
    }
}
