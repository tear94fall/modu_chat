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
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.entity.ChatRoom;
import com.example.modumessenger.RoomDatabase.Database.ChatRoomDatabase;
import com.example.modumessenger.RoomDatabase.Entity.ChatRoomEntity;
import com.example.modumessenger.entity.Member;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {

    List<ChatRoom> chatRoomList;

    public ChatRoomAdapter(List<ChatRoom> chatRoomList) {
        this.chatRoomList = (chatRoomList == null || chatRoomList.size() == 0) ? new ArrayList<>() : chatRoomList;
        sortChatRoom();
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

        holder.setDatabase(chatRoom);
        holder.setChatRoomTitle(chatRoom);
        holder.setChatRoomInfo(chatRoom);
        holder.setChatRoomImage(chatRoom);
        holder.setChatRoomClickEvent(chatRoom);
    }

    public void sortChatRoom() {
        this.chatRoomList.sort(Comparator.comparing(ChatRoom::getLastChatTime, Comparator.reverseOrder()));
    }

    @Override
    public int getItemCount() {
        return this.chatRoomList.size();
    }

    public static class ChatRoomViewHolder extends RecyclerView.ViewHolder {

        String userId;
        ChatRoomDatabase chatRoomDB;
        TextView chatRoomName;
        TextView lastChatMessage;
        TextView lastChatTime;
        ImageView chatRoomImage;
        ConstraintLayout chatRoomCardView;

        public ChatRoomViewHolder(@NonNull View itemView) {
            super(itemView);
            userId = PreferenceManager.getString("userId");
            chatRoomDB = ChatRoomDatabase.getInstance(this.itemView.getContext());
            chatRoomName = itemView.findViewById(R.id.chat_room_name);
            lastChatMessage = itemView.findViewById(R.id.last_chat_message);
            lastChatTime = itemView.findViewById(R.id.last_chat_time);
            chatRoomImage = itemView.findViewById(R.id.chat_room_image);
            chatRoomCardView = itemView.findViewById(R.id.chatRoomCardViewLayout);
        }

        public void setDatabase(ChatRoom chatRoom) {
            ChatRoomEntity chatRoomEntity = new ChatRoomEntity(chatRoom);
            chatRoomDB.chatRoomDao().update(chatRoomEntity);
        }

        public void setChatRoomTitle(ChatRoom chatRoom) {
            if (chatRoom.getMembers().size() == 1) {
                this.chatRoomName.setText(chatRoom.getRoomName());
            } else if (chatRoom.getMembers().size() == 2) {
                chatRoom.getMembers().forEach(member -> {
                    if (!member.getUserId().equals(userId)) {
                        this.chatRoomName.setText(member.getUsername());
                    }
                });
            } else if (chatRoom.getMembers().size() > 2) {
                List<String> userIds = chatRoom.getMembers().stream()
                        .map(Member::getUsername)
                        .collect(Collectors.toList());
                String title = String.join(", ", userIds);
                title = title.substring(0, Math.min(title.length(), 25));
                this.chatRoomName.setText(title);
            } else {
                this.chatRoomName.setText(chatRoom.getRoomName());
            }
        }

        public void setChatRoomInfo(ChatRoom chatRoom) {
            this.lastChatMessage.setText(chatRoom.getLastChatMsg());
            this.lastChatTime.setText(getShortTime(chatRoom.getLastChatTime()));
        }

        public void setChatRoomImage(ChatRoom chatRoom) {
            if (chatRoom.getMembers().size() == 1) {
                setGlide(R.drawable.basic_chat_room_image);
            } else if(chatRoom.getMembers().size() == 2) {
                chatRoom.getMembers().forEach(member -> {
                    if(!member.getUserId().equals(userId)) {
                        setGlide(member.getProfileImage());
                    }
                });
            } else if (chatRoom.getMembers().size() > 2) {
                setGlide(R.drawable.basic_chat_room_image);
            } else {
                if (chatRoom.getRoomImage() != null && !chatRoom.getRoomImage().equals("")) {
                    setGlide(chatRoom.getRoomImage());
                } else {
                    setGlide(null);
                }
            }
        }

        public void setGlide(int resources) {
            Glide.with(chatRoomImage)
                    .load(resources)
                    .error(Glide.with(chatRoomImage)
                            .load(R.drawable.basic_profile_image)
                            .into(chatRoomImage))
                    .into(chatRoomImage);
        }

        public void setGlide(String imageUrl) {
            if(imageUrl != null && !imageUrl.equals("")) {
                Glide.with(chatRoomImage)
                        .load(imageUrl)
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

        public String getShortTime(String time) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(time, formatter);
            return dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        }
    }
}
