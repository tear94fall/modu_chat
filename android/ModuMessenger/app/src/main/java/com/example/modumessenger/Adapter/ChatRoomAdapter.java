package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;
import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

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
import com.example.modumessenger.Global.ChatRoomNameUtil;
import com.example.modumessenger.R;
import com.example.modumessenger.entity.ChatRoom;
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

        holder.setChatRoomTitle(chatRoom);
        holder.setChatRoomLastMsg(chatRoom);
        holder.setChatRoomLastTime(chatRoom);
        holder.setChatBadge(chatRoom);
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

    public void setChatRoomList(List<ChatRoom> rooms) {
        this.chatRoomList.clear();
        this.chatRoomList.addAll(rooms);
        sortChatRoom();
        notifyDataSetChanged();
    }

    public static class ChatRoomViewHolder extends RecyclerView.ViewHolder {
        FragmentChat fragmentChat;

        Member member;
        String userId;
        String username;
        TextView chatRoomName;
        TextView lastChatMessage;
        TextView lastChatTime;
        TextView chatBadge;
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

            member = getDataStoreMember();

            userId = member.getUserId();
            username = member.getUsername();
            chatRoomName = itemView.findViewById(R.id.chat_room_name);
            lastChatMessage = itemView.findViewById(R.id.last_chat_message);
            lastChatTime = itemView.findViewById(R.id.last_chat_time);
            chatBadge = itemView.findViewById(R.id.chat_badge);
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

        public void setChatRoomTitle(ChatRoom chatRoom) {
            this.chatRoomName.setText(ChatRoomNameUtil.resolve(
                    chatRoom.getRoomName(), chatRoom.getMembers(), userId, username, 25));
        }

        public void setChatRoomLastMsg(ChatRoom chatRoom) {
            this.lastChatMessage.setText(chatTypeMap.containsKey(chatRoom.getLastChatMsg()) ? chatTypeMap.get(chatRoom.getLastChatMsg()) : chatRoom.getLastChatMsg());
        }

        public void setChatRoomLastTime(ChatRoom chatRoom) {
            this.lastChatTime.setText(chatRoom.getLastChatTime().equals("") || chatRoom.getLastChatMsg().equals("") ? "" : getShortTime(chatRoom.getLastChatTime()));
        }

        public void setChatBadge(ChatRoom chatRoom) {
            int count = chatRoom.getUnreadCount();

            if (count <= 0) {
                // INVISIBLE 은 자리를 차지해 레이아웃이 어긋난다.
                this.chatBadge.setVisibility(View.GONE);
                return;
            }

            this.chatBadge.setText(count > 999 ? "999+" : String.valueOf(count));
            this.chatBadge.setVisibility(View.VISIBLE);
        }

        public void setChatRoomImage(ChatRoom chatRoom) {
            List<ImageView> imageViewList = Arrays.asList(memberImage1, memberImage2, memberImage3, memberImage4);
            List<CardView> cardViewList = Arrays.asList(memberImageCardView1, memberImageCardView2, memberImageCardView3, memberImageCardView4);

            // 재사용되는 뷰라 매번 처음 상태로 되돌린다. 예전에는 뷰를 만들 때 한 번만
            // 숨겨서, 여러 명 방을 보여 준 자리를 재사용하면 작은 사진들이 남았다.
            chatRoomImage.setVisibility(View.VISIBLE);
            for (int index = 0; index < imageViewList.size(); index++) {
                imageViewList.get(index).setVisibility(View.INVISIBLE);
                cardViewList.get(index).setVisibility(View.INVISIBLE);
            }

            // 직접 넣은 방 사진이 있으면 참여자 수와 상관없이 그것을 쓴다.
            // 예전에는 참여자가 셋 이상인 방에서만 방 사진을 봐서, 1:1 방은
            // 사진을 바꿔도 목록에 반영되지 않았다.
            if (ChatRoomNameUtil.hasCustomImage(chatRoom.getRoomImage())) {
                setProfileImage(chatRoomImage, chatRoom.getRoomImage());
                return;
            }

            List<Member> others = chatRoom.getMembers().stream()
                    .filter(m -> m.getUserId() != null && !m.getUserId().equals(userId))
                    .collect(Collectors.toList());

            if (others.isEmpty()) {
                setProfileImage(chatRoomImage, chatRoom.getMembers().isEmpty()
                        ? "" : chatRoom.getMembers().get(0).getProfileImage());
                return;
            }

            if (others.size() == 1) {
                setProfileImage(chatRoomImage, others.get(0).getProfileImage());
                return;
            }

            chatRoomImage.setVisibility(View.INVISIBLE);
            for (int index = 0; index < Math.min(4, others.size()); index++) {
                imageViewList.get(index).setVisibility(View.VISIBLE);
                cardViewList.get(index).setVisibility(View.VISIBLE);
                setProfileImage(imageViewList.get(index), others.get(index).getProfileImage());
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
