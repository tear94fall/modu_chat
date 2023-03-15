package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.GlideUtil.setProfileImage;
import static com.example.modumessenger.Global.SharedPrefHelper.getSharedObjectMember;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.Activity.ProfileImageActivity;
import com.example.modumessenger.R;
import com.example.modumessenger.dto.ChatType;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.RoomDatabase.Database.ChatDatabase;
import com.example.modumessenger.RoomDatabase.Entity.ChatEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public final int LEFT = 1;
    public final int RIGHT = 2;
    public final int LEFT_DUP = 3;
    public final int RIGHT_DUP = 4;
    public final int LEFT_IMAGE = 5;
    public final int RIGHT_IMAGE = 6;
    private final int LEFT_IMAGE_DUP = 5;
    private final int RIGHT_IMAGE_DUP = 6;

    private final List<Member> memberList;
    private final List<ChatBubble> chatList;

    Member myInfo;
    ChatDatabase chatDatabase;

    public ChatHistoryAdapter(List<ChatBubble> chatList, List<Member> memberList) {
        this.memberList = (memberList == null || memberList.size() == 0) ? new ArrayList<>() : memberList;
        this.chatList = chatList;
        this.myInfo = getSharedObjectMember();

        sortChatBubble();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        chatDatabase = ChatDatabase.getInstance(parent.getContext());

        if (viewType == LEFT) {
            view = inflater.inflate(R.layout.chat_bubble_left, parent, false);
            return new ChatHistoryAdapter.ChatBubbleLeftViewHolder(view);
        } else if (viewType == RIGHT) {
            view = inflater.inflate(R.layout.chat_bubble_right, parent, false);
            return new ChatHistoryAdapter.ChatBubbleRightViewHolder(view);
        } else if (viewType == LEFT_DUP) {
            view = inflater.inflate(R.layout.chat_bubble_left_dup, parent, false);
            return new ChatHistoryAdapter.ChatBubbleLeftDuplicateViewHolder(view);
        } else if (viewType == RIGHT_DUP) {
            view = inflater.inflate(R.layout.chat_bubble_right_dup, parent, false);
            return new ChatHistoryAdapter.ChatBubbleRightDuplicateViewHolder(view);
        } else if (viewType == LEFT_IMAGE) {
            view = inflater.inflate(R.layout.chat_bubble_left_image, parent, false);
            return new ChatHistoryAdapter.ChatBubbleLeftImageViewHolder(view);
        } else if (viewType == RIGHT_IMAGE) {
            view = inflater.inflate(R.layout.chat_bubble_right_image, parent, false);
            return new ChatBubbleRightImageViewHolder(view);
        }  else {
            view = inflater.inflate(R.layout.chat_bubble_right, parent, false);
            return new ChatHistoryAdapter.ChatBubbleRightViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatBubble chat = chatList.get(position);
        ChatEntity chatEntity = new ChatEntity(chat);
        chatDatabase.chatDao().update(chatEntity);

        if (holder instanceof ChatBubbleLeftViewHolder) {
            ChatBubbleLeftViewHolder leftHolder = ((ChatBubbleLeftViewHolder) holder);

            Member member = getChatSendMember(chat.getSender());
            setProfileImage((leftHolder.senderImage), member.getProfileImage());

            leftHolder.setUserClickEvent(member);
            leftHolder.chatSender.setText(member.getEmail());
            leftHolder.chatMessage.setText(chat.getChatMsg());
            leftHolder.leftChatTime.setText(getShortTime(chat.getChatTime()));
        } else if (holder instanceof ChatBubbleLeftImageViewHolder) {
            ChatBubbleLeftImageViewHolder leftImageViewHolder = ((ChatBubbleLeftImageViewHolder) holder);
            Member member = getChatSendMember(chat.getSender());

            setProfileImage((leftImageViewHolder.senderImage), member.getProfileImage());
            setProfileImage(leftImageViewHolder.chatImage, chat.getChatMsg());

            leftImageViewHolder.setUserClickEvent(member);
            leftImageViewHolder.setChatImageClickEvent(chat);
            leftImageViewHolder.chatSender.setText(member.getEmail());
            leftImageViewHolder.leftChatTime.setText(getShortTime(chat.getChatTime()));
        } else if (holder instanceof ChatBubbleLeftDuplicateViewHolder) {
            ChatBubbleLeftDuplicateViewHolder leftDupHolder = ((ChatBubbleLeftDuplicateViewHolder) holder);
            leftDupHolder.chatMessage.setText(chat.getChatMsg());
            leftDupHolder.leftChatTime.setVisibility(View.INVISIBLE);
        } else if (holder instanceof ChatBubbleRightViewHolder) {
            ChatBubbleRightViewHolder rightHolder = ((ChatBubbleRightViewHolder) holder);
            rightHolder.chatMessage.setText(chat.getChatMsg());
            rightHolder.rightChatTime.setText(getShortTime(chat.getChatTime()));
        } else if (holder instanceof ChatBubbleRightImageViewHolder) {
            ChatBubbleRightImageViewHolder rightImageViewHolder = ((ChatBubbleRightImageViewHolder) holder);

            setProfileImage(rightImageViewHolder.chatImage, chat.getChatMsg());
            rightImageViewHolder.setChatImageClickEvent(chat);
            rightImageViewHolder.rightChatTime.setText(getShortTime(chat.getChatTime()));
        } else if (holder instanceof ChatBubbleRightDuplicateViewHolder) {
            ChatBubbleRightDuplicateViewHolder rightDupHolder = ((ChatBubbleRightDuplicateViewHolder) holder);
            rightDupHolder.chatMessage.setText(chat.getChatMsg());
            rightDupHolder.leftChatTime.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatBubble chat = chatList.get(position);
        ChatBubble prevChat = chatList.get(Math.max(0, position - 1));

        if (!chat.getSender().equals(myInfo.getUserId())) {
            if (position > 0 &&
                    chat.getSender().equals(prevChat.getSender()) &&
                    getShortTime(chat.getChatTime()).equals(getShortTime(prevChat.getChatTime()))) {

                return chat.getChatType() == ChatType.CHAT_TYPE_TEXT ? LEFT_DUP : LEFT_IMAGE_DUP;
            }

            return chat.getChatType() == ChatType.CHAT_TYPE_TEXT ? LEFT : LEFT_IMAGE;
        } else {
            if (position > 0 && getShortTime(chat.getChatTime()).equals(getShortTime(prevChat.getChatTime()))) {
                return chat.getChatType() == ChatType.CHAT_TYPE_TEXT ? RIGHT_DUP : RIGHT_IMAGE_DUP;
            }
        }

        return chat.getChatType() == ChatType.CHAT_TYPE_TEXT ? RIGHT : RIGHT_IMAGE;
    }

    public void sortChatBubble() {
        this.chatList.sort(Comparator.comparing(ChatBubble::getChatTime, Comparator.naturalOrder()));
    }

    public Member getChatSendMember(String sender) {
        return memberList.stream()
                .filter(chatRoomMember -> chatRoomMember.getUserId().equals(sender))
                .findFirst()
                .orElse(null);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addChatMsg(ChatBubble chatBubble) {
        this.chatList.add(chatBubble);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addChatMsgBack(List<ChatBubble> chatList) {
        this.chatList.addAll(chatList);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addChatMsgFront(List<ChatBubble> chatList) {
        chatList.forEach(chatBubble -> {
            this.chatList.add(0, chatBubble);
        });

        notifyDataSetChanged();
    }

    public int getRightChatBubbleType(int chatContentType) {
        /*
        채팅 타입 정리
        RIGHT_SINGLE (RS) - 시간, 메시지 내용
        RIGHT_HEADER (RH) - 메시지 내용
        RIGHT_TAIL (RT) - 시간, 메시지 내용

        현재 채팅은 내가 보낸 채팅
        1) 가장 처음에 위치한 채팅 인경우
            1.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우
                - RS
            1.2) 다음 채팅이 내가 보낸 채팅인 경우
                1.2.1) 전송 시간이 동일한 경우
                    - RH
                1.1.2) 전송 시간이 다른 경우
                    - RS
        2) 가장 끝에 위치한 채팅 경우
            2.1) 이전 채팅이 다른 사람이 보낸 채팅인 경우
                - RS
            2.2) 이전 채팅이 내가 보낸 채팅인 경우
                2.2.1) 전송 시간이 동일한 경우
                    - RT
                2.2.2) 전송 시간이 다른 경우
                    - RS
        3) 중간에 위치한 채팅인 경우
            3.1) 이전 채팅이 다른 사람이 보낸 채팅인 경우
                3.1.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우
                    - RS
                3.1.2) 다음 채팅이 내가 보낸 채팅인 경우
                    3.1.2.1) 전송 시간이 동일한 경우
                        - RH
                    3.1.2.2) 전송 시간이 다른 경우
                        - RS
            3.2) 이전 채팅이 내가 보낸 채팅인 경우
                3.2.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우
                    - RT
                3.2.2) 다음 채팅이 내가 보낸 채팅인 경우
                    3.2.2.1) 전송 시간이 동일한 경우
                        - RH
                    3.2.2.2) 전송 시간이 다른 경우
                        - RT
         */

        return ChatBubbleType.RIGHT_TEXT_TAIL.getType();
    }

    public int getLeftChatBubbleType(int chatContentType) {
        /*
        채팅 타입 정리
        LEFT_SINGLE (LS) - 프로필 사진, 이름, 시간, 메시지 내용
        LEFT_HEADER (LH) - 프로필 사진, 이름, 메시지 내용
        LEFT_BODY (LB) - 메시지 내용
        LEFT_TAIL (LT) - 시간, 메시지 내용

        현재 채팅은 다른 사람이 보낸 채팅
        1) 가장 처음에 위치한 채팅 인경우
            1.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우
                - RS
            1.2) 다음 채팅과 보낸 사람이 같은 경우
                1.2.1) 전송 시간이 동일한 경우
                    - LH
                1.1.2) 전송 시간이 다른 경우
                    - LS
        2) 가장 끝에 위치한 채팅 경우
            2.1) 이전 채팅이 다른 사람이 보낸 채팅인 경우
                - RS
            2.2) 이전 채팅과 보낸 사람이 같은 경우
                2.2.1) 전송 시간이 동일한 경우
                    - LT
                2.2.2) 전송 시간이 다른 경우
                    - LS
        3) 중간에 위치한 채팅인 경우
            3.1) 이전 채팅이 다른 사람이 보낸 채팅인 경우
                3.1.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우
                    - LS
                3.1.2) 다음 채팅과 보낸 사람이 같은 경우
                    2.2.1) 전송 시간이 동일한 경우
                        - LH
                    2.2.2) 전송 시간이 다른 경우
                        - LS
            3.2) 이전 채팅과 보낸 사람이 같은 경우
                3.2.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우
                    - LT
                3.2.2) 다음 채팅과 보낸 사람이 같은 경우
                    2.2.1) 전송 시간이 동일한 경우
                        - LB
                    2.2.2) 전송 시간이 다른 경우
                        - LT

         */

        return ChatBubbleType.LEFT_TEXT_TAIL.getType();
    }

    public String getShortTime(String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(time, formatter);
        return dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
    }

    public void startProfileActivity(View view, Member member) {
        Intent intent = new Intent(view.getContext(), ProfileActivity.class);
        intent.putExtra("email", member.getEmail());
        intent.putExtra("userId", member.getUserId());

        view.getContext().startActivity(intent);
    }

    public class ChatBubbleLeftViewHolder extends RecyclerView.ViewHolder {
        ImageView senderImage;
        TextView chatMessage;
        TextView leftChatTime;
        TextView chatSender;

        public ChatBubbleLeftViewHolder(@NonNull View itemView) {
            super(itemView);
            senderImage = itemView.findViewById(R.id.message_sender_image);
            chatMessage = itemView.findViewById(R.id.left_chat_text);
            chatSender = itemView.findViewById(R.id.message_sender);
            leftChatTime = itemView.findViewById(R.id.left_chat_time);
        }

        public void setUserClickEvent(Member member) {
            this.senderImage.setOnClickListener(v -> {
                startProfileActivity(v, member);
            });
        }
    }

    public class ChatBubbleLeftImageViewHolder extends RecyclerView.ViewHolder {
        ImageView senderImage;
        ImageView chatImage;
        TextView leftChatTime;
        TextView chatSender;

        public ChatBubbleLeftImageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderImage = itemView.findViewById(R.id.message_sender_image);
            chatImage = itemView.findViewById(R.id.left_chat_image);
            chatSender = itemView.findViewById(R.id.message_sender);
            leftChatTime = itemView.findViewById(R.id.left_chat_time);
        }

        public void setUserClickEvent(Member member) {
            this.senderImage.setOnClickListener(v -> {
                startProfileActivity(v, member);
            });
        }

        public void setChatImageClickEvent(ChatBubble chat) {
            this.chatImage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileImageActivity.class);

                ArrayList<String> imageFileList = new ArrayList<>();
                imageFileList.add(chat.getChatMsg());
                intent.putStringArrayListExtra("imageFileList", imageFileList);

                v.getContext().startActivity(intent);
            });
        }
    }

    public static class ChatBubbleLeftDuplicateViewHolder extends RecyclerView.ViewHolder {
        TextView chatMessage;
        TextView leftChatTime;

        public ChatBubbleLeftDuplicateViewHolder(@NonNull View itemView) {
            super(itemView);
            chatMessage = itemView.findViewById(R.id.left_chat_text);
            leftChatTime = itemView.findViewById(R.id.left_chat_time);
        }
    }

    public static class ChatBubbleRightViewHolder extends RecyclerView.ViewHolder {
        TextView chatMessage;
        TextView rightChatTime;

        public ChatBubbleRightViewHolder(@NonNull View itemView) {
            super(itemView);
            chatMessage = itemView.findViewById(R.id.right_chat_text);
            rightChatTime = itemView.findViewById(R.id.right_chat_time);
        }
    }

    public static class ChatBubbleRightImageViewHolder extends RecyclerView.ViewHolder {
        ImageView chatImage;
        TextView rightChatTime;

        public ChatBubbleRightImageViewHolder(@NonNull View itemView) {
            super(itemView);
            chatImage = itemView.findViewById(R.id.right_chat_image);
            rightChatTime = itemView.findViewById(R.id.right_chat_time);
        }

        public void setChatImageClickEvent(ChatBubble chat) {
            this.chatImage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileImageActivity.class);

                ArrayList<String> imageFileList = new ArrayList<>();
                imageFileList.add(chat.getChatMsg());
                intent.putStringArrayListExtra("imageFileList", imageFileList);

                v.getContext().startActivity(intent);
            });
        }
    }

    public static class ChatBubbleRightDuplicateViewHolder extends RecyclerView.ViewHolder {
        TextView chatMessage;
        TextView leftChatTime;

        public ChatBubbleRightDuplicateViewHolder(@NonNull View itemView) {
            super(itemView);
            chatMessage = itemView.findViewById(R.id.right_chat_text);
            leftChatTime = itemView.findViewById(R.id.right_chat_time);
        }
    }
}
