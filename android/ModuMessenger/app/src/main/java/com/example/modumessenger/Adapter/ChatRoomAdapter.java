package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.GlideUtil.setProfileImage;
import static com.example.modumessenger.Global.SharedPrefHelper.getSharedObjectMember;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.ChatActivity;
import com.example.modumessenger.Fragments.FragmentChat;
import com.example.modumessenger.R;
import com.example.modumessenger.entity.ChatRoom;
import com.example.modumessenger.RoomDatabase.Database.ChatRoomDatabase;
import com.example.modumessenger.RoomDatabase.Entity.ChatRoomEntity;
import com.example.modumessenger.entity.Member;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.ChatRoomViewHolder> {

    public final int NORMAL_CHAT = 1;
    public final int GROUP_CHAT = 2;

    FragmentChat fragmentChat;
    List<ChatRoom> chatRoomList;

    public ChatRoomAdapter(List<ChatRoom> chatRoomList, FragmentChat fragmentChat) {
        this.fragmentChat = fragmentChat;
        this.chatRoomList = (chatRoomList == null || chatRoomList.size() == 0) ? new ArrayList<>() : chatRoomList;
        sortChatRoom();
    }

    @NonNull
    @Override
    public ChatRoomAdapter.ChatRoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.chat_room_row, parent, false);
        return new ChatRoomAdapter.ChatRoomViewHolder(this.fragmentChat, view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatRoomAdapter.ChatRoomViewHolder holder, int position) {
        ChatRoom chatRoom = this.chatRoomList.get(position);

        holder.setDatabase(chatRoom);
        holder.setChatRoomTitle(chatRoom);
        holder.setChatRoomLastMsg(chatRoom);
        holder.setChatRoomLastTime(chatRoom);
        holder.setChatRoomImage(chatRoom);
        holder.setChatRoomClickEvent(chatRoom);
    }

    @Override
    public int getItemCount() {
        return this.chatRoomList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return chatRoomList.get(position).getMembers().size() > 2 ? NORMAL_CHAT : GROUP_CHAT;
    }

    public void sortChatRoom() {
        this.chatRoomList.sort(Comparator.comparing(ChatRoom::getLastChatTime, Comparator.reverseOrder()));
    }

    public static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        FragmentChat fragmentChat;

        Member member;
        String userId;
        String username;
        ChatRoomDatabase chatRoomDB;
        TextView chatRoomName;
        TextView lastChatMessage;
        TextView lastChatTime;
        ImageView chatRoomImage;
        ConstraintLayout chatRoomCardView;
        ImageView memberImage1, memberImage2, memberImage3, memberImage4;
        CardView memberImageCardView1, memberImageCardView2, memberImageCardView3, memberImageCardView4;

        Map<String, String> chatTypeMap = new HashMap<String, String>() {
            {
                put("image", "사진");
                put("file", "파일");
                put("audio", "음성");
            }
        };

        public ChatRoomViewHolder(FragmentChat fragmentChat, @NonNull View itemView) {
            super(itemView);
            this.fragmentChat = fragmentChat;

            this.member = getSharedObjectMember();

            userId = member.getUserId();
            username = member.getUsername();
            chatRoomDB = ChatRoomDatabase.getInstance(this.itemView.getContext());
            chatRoomName = itemView.findViewById(R.id.chat_room_name);
            lastChatMessage = itemView.findViewById(R.id.last_chat_message);
            lastChatTime = itemView.findViewById(R.id.last_chat_time);
            chatRoomImage = itemView.findViewById(R.id.chat_room_image);
            chatRoomCardView = itemView.findViewById(R.id.chatRoomCardViewLayout);

            memberImage1 = itemView.findViewById(R.id.chat_room_image1);
            memberImage2 = itemView.findViewById(R.id.chat_room_image2);
            memberImage3 = itemView.findViewById(R.id.chat_room_image3);
            memberImage4 = itemView.findViewById(R.id.chat_room_image4);

            memberImageCardView1 = itemView.findViewById(R.id.chat_room_image_card_view1);
            memberImageCardView2 = itemView.findViewById(R.id.chat_room_image_card_view2);
            memberImageCardView3 = itemView.findViewById(R.id.chat_room_image_card_view3);
            memberImageCardView4 = itemView.findViewById(R.id.chat_room_image_card_view4);

            memberImage1.setVisibility(View.INVISIBLE);
            memberImage2.setVisibility(View.INVISIBLE);
            memberImage3.setVisibility(View.INVISIBLE);
            memberImage4.setVisibility(View.INVISIBLE);

            memberImageCardView1.setVisibility(View.INVISIBLE);
            memberImageCardView2.setVisibility(View.INVISIBLE);
            memberImageCardView3.setVisibility(View.INVISIBLE);
            memberImageCardView4.setVisibility(View.INVISIBLE);
        }

        public void setDatabase(ChatRoom chatRoom) {
            ChatRoomEntity chatRoomEntity = new ChatRoomEntity(chatRoom);
            chatRoomDB.chatRoomDao().update(chatRoomEntity);
        }

        public void setChatRoomTitle(ChatRoom chatRoom) {
            if (chatRoom.getMembers().size() == 1) {
                this.chatRoomName.setText(chatRoom.getMembers().get(0).getUserId().equals(userId) ? String.format("나와의 채팅 (%s)", username) : chatRoom.getRoomName());
            } else if (chatRoom.getMembers().size() == 2) {
                chatRoom.getMembers().forEach(member -> {
                    if (!member.getUserId().equals(userId)) {
                        this.chatRoomName.setText(member.getUsername());
                    }
                });
            } else if (chatRoom.getMembers().size() > 2) {
                List<String> userIds = chatRoom.getMembers().stream()
                        .map(Member::getUsername)
                        .filter(name -> !name.equals(username))
                        .collect(Collectors.toList());
                String title = String.join(", ", userIds);
                title = title.substring(0, Math.min(title.length(), 25)).trim();
                title = title.substring(0, Math.min(title.length(), title.endsWith(",") ? title.length() - 1 : title.length()));
                this.chatRoomName.setText(title);
            } else {
                this.chatRoomName.setText(chatRoom.getRoomName());
            }
        }

        public void setChatRoomLastMsg(ChatRoom chatRoom) {
            this.lastChatMessage.setText(chatTypeMap.containsKey(chatRoom.getLastChatMsg()) ? chatTypeMap.get(chatRoom.getLastChatMsg()) : chatRoom.getLastChatMsg());
        }

        public void setChatRoomLastTime(ChatRoom chatRoom) {
            this.lastChatTime.setText(chatRoom.getLastChatTime().equals("") || chatRoom.getLastChatMsg().equals("") ? "" : getShortTime(chatRoom.getLastChatTime()));
        }

        public void setChatRoomImage(ChatRoom chatRoom) {
            if (chatRoom.getMembers().size() == 1) {
                setProfileImage(chatRoomImage, chatRoom.getMembers().get(0).getProfileImage());
            } else if(chatRoom.getMembers().size() == 2) {
                chatRoom.getMembers().forEach(member -> {
                    if(!member.getUserId().equals(userId)) {
                        setProfileImage(chatRoomImage, member.getProfileImage());
                    }
                });
            } else if (chatRoom.getMembers().size() > 2) {
                if (chatRoom.getRoomImage() != null && !chatRoom.getRoomImage().equals("")) {
                    setProfileImage(chatRoomImage, chatRoom.getRoomImage());
                } else {
                    List<Member> memberList = chatRoom.getMembers().stream()
                            .filter(m -> !m.getUserId().equals(userId))
                            .collect(Collectors.toList());

                    chatRoomImage.setVisibility(View.INVISIBLE);
                    List<ImageView> imageViewList = new ArrayList<>(Arrays.asList(memberImage1, memberImage2, memberImage3, memberImage4));
                    List<CardView> cardViewList = new ArrayList<>(Arrays.asList(memberImageCardView1, memberImageCardView2, memberImageCardView3, memberImageCardView4));

                    for(int index=0; index<Math.min(4, memberList.size()); index++) {
                        imageViewList.get(index).setVisibility(View.VISIBLE);
                        cardViewList.get(index).setVisibility(View.VISIBLE);
                        setProfileImage(imageViewList.get(index), memberList.get(index).getProfileImage());
                    }
                }
            } else {
                setProfileImage(chatRoomImage, chatRoom.getRoomImage());
            }
        }

        public void setChatRoomClickEvent(ChatRoom chatRoom) {
            this.chatRoomCardView.setOnClickListener(view -> {
                Intent intent = new Intent(view.getContext(), ChatActivity.class);
                intent.putExtra("roomId", chatRoom.getRoomId());
                view.getContext().startActivity(intent);
            });

            this.chatRoomCardView.setOnLongClickListener(view -> {
                this.fragmentChat.showChatRoomPopupMenu(view, chatRoom);
                return false;
            });
        }

        public String getShortTime(String time) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = LocalDateTime.parse(time, formatter);
            return dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
        }
    }
}
