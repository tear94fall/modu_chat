package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Adapter.ChatBubbleType.*;
import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;
import static com.example.modumessenger.Global.GlideUtil.setProfileImage;
import static com.example.modumessenger.dto.ChatType.*;

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

import com.example.modumessenger.Activity.ChatImageActivity;
import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.R;
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

    private final List<Member> memberList;
    private final List<ChatBubble> chatList;

    Member myInfo;
    ChatDatabase chatDatabase;

    public ChatHistoryAdapter(List<ChatBubble> chatList, List<Member> memberList) {
        this.memberList = (memberList == null || memberList.size() == 0) ? new ArrayList<>() : memberList;
        this.chatList = chatList;
        myInfo = getDataStoreMember();

        sortChatBubble();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        RecyclerView.ViewHolder viewHolder;
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        chatDatabase = ChatDatabase.getInstance(parent.getContext());

        if (viewType == RIGHT_TEXT_SINGLE.getType() || viewType == RIGHT_TEXT_HEADER.getType() || viewType == RIGHT_TEXT_TAIL.getType()) {
            viewHolder = new ChatBubbleRightTextViewHolder(inflater.inflate(R.layout.chat_bubble_right_text, parent, false), viewType);
        } else if(viewType == RIGHT_IMAGE_SINGLE.getType() || viewType == RIGHT_IMAGE_HEADER.getType() || viewType == RIGHT_IMAGE_TAIL.getType()) {
            viewHolder = new ChatBubbleRightImageViewHolder(inflater.inflate(R.layout.chat_bubble_right_image, parent, false), viewType);
        } else if (viewType == LEFT_TEXT_SINGLE.getType() || viewType == LEFT_TEXT_HEADER.getType()) {
            viewHolder = new ChatBubbleLeftTextViewHolder(inflater.inflate(R.layout.chat_bubble_left_text, parent, false), viewType);
        } else if (viewType == LEFT_TEXT_BODY.getType() || viewType == LEFT_TEXT_TAIL.getType()) {
            viewHolder = new ChatBubbleLeftTextOnlyViewHolder(inflater.inflate(R.layout.chat_bubble_left_text_only, parent, false), viewType);
        } else if (viewType == LEFT_IMAGE_SINGLE.getType() || viewType == LEFT_IMAGE_HEADER.getType()) {
            viewHolder = new ChatBubbleLeftImageViewHolder(inflater.inflate(R.layout.chat_bubble_left_image, parent, false), viewType);
        } else if (viewType == LEFT_IMAGE_BODY.getType() || viewType == LEFT_IMAGE_TAIL.getType()) {
            viewHolder = new ChatBubbleLeftImageOnlyViewHolder(inflater.inflate(R.layout.chat_bubble_left_image_only, parent, false), viewType);
        } else {
            viewHolder = new ChatBubbleRightTextViewHolder(inflater.inflate(R.layout.chat_bubble_right_text, parent, false), viewType);
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatBubble chat = chatList.get(position);
        ChatEntity chatEntity = new ChatEntity(chat);
        chatDatabase.chatDao().update(chatEntity);

        if (holder instanceof ChatBubbleLeftTextViewHolder) {
            ChatBubbleLeftTextViewHolder leftText = ((ChatBubbleLeftTextViewHolder) holder);
            leftText.setChatBubble(chat);
        } else if (holder instanceof ChatBubbleLeftTextOnlyViewHolder) {
            ChatBubbleLeftTextOnlyViewHolder leftTextOnly = ((ChatBubbleLeftTextOnlyViewHolder) holder);
            leftTextOnly.setChatBubble(chat);
        } else if (holder instanceof ChatBubbleLeftImageViewHolder) {
            ChatBubbleLeftImageViewHolder leftImage = ((ChatBubbleLeftImageViewHolder) holder);
            leftImage.setChatBubble(chat);
        } else if (holder instanceof ChatBubbleLeftImageOnlyViewHolder) {
            ChatBubbleLeftImageOnlyViewHolder leftImageOnly = ((ChatBubbleLeftImageOnlyViewHolder) holder);
            leftImageOnly.setChatBubble(chat);
        } else if (holder instanceof ChatBubbleRightTextViewHolder) {
            ChatBubbleRightTextViewHolder rightText = ((ChatBubbleRightTextViewHolder) holder);
            rightText.setChatBubble(chat);
        } else if (holder instanceof ChatBubbleRightImageViewHolder) {
            ChatBubbleRightImageViewHolder rightImage = ((ChatBubbleRightImageViewHolder) holder);
            rightImage.setChatBubble(chat);
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return (!chatList.get(position).getSender().equals(myInfo.getUserId())) ? getLeftChatBubbleType(position) : getRightChatBubbleType(position);
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

    public int getRightChatBubbleType(int position) {
        /*
        채팅 타입 정리
        RIGHT_SINGLE (RS) - 시간, 메시지 내용
        RIGHT_HEADER (RH) - 메시지 내용
        RIGHT_TAIL (RT) - 시간, 메시지 내용

        현재 채팅은 내가 보낸 채팅
        1) 가장 처음에 위치한 채팅 인경우
            1.0) 총 채팅이 1개인 경우 - RS
            1.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우 - RS
            1.2) 다음 채팅이 내가 보낸 채팅인 경우
                1.1.1) 전송 시간이 다른 경우 - RS
                1.2.2) 전송 시간이 동일한 경우 - RH
        2) 가장 끝에 위치한 채팅 경우
            2.1) 이전 채팅이 다른 사람이 보낸 채팅인 경우 - RS
            2.2) 이전 채팅이 내가 보낸 채팅인 경우
                2.2.1) 전송 시간이 다른 경우 - RS
                2.2.2) 전송 시간이 동일한 경우 - RT
        3) 중간에 위치한 채팅인 경우
            3.1) 이전 채팅이 다른 사람이 보낸 채팅인 경우
                3.1.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우 - RS
                3.1.2) 다음 채팅이 내가 보낸 채팅인 경우
                    3.1.2.1) 전송 시간이 다른 경우 - RS
                    3.1.2.2) 전송 시간이 동일한 경우 - RH
            3.2) 이전 채팅이 내가 보낸 채팅인 경우
                3.2.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우 - RT
                3.2.2) 다음 채팅이 내가 보낸 채팅인 경우
                    3.2.2.1) 전송 시간이 다른 경우 - RT
                    3.2.2.2) 전송 시간이 동일한 경우 - RH
         */

        ChatBubbleType type = INVALID;
        ChatBubble currentChat = chatList.get(position);

        if (position == 0) {
            if(getItemCount() == 1) {
                type = RIGHT_TEXT_SINGLE;
            } else {
                ChatBubble nextChat = chatList.get(position + 1);

                if (!currentChat.getSender().equals(nextChat.getSender())) {
                    type = RIGHT_TEXT_SINGLE;
                } else {
                    type = (!equalShotTime(currentChat.getChatTime(), nextChat.getChatTime())) ? RIGHT_TEXT_SINGLE : RIGHT_TEXT_HEADER;
                }
            }
        } else if (position == getItemCount() - 1) {
            ChatBubble prevChat = chatList.get(position - 1);

            if (!currentChat.getSender().equals(prevChat.getSender())) {
                type = RIGHT_TEXT_SINGLE;
            } else {
                type = (!equalShotTime(currentChat.getChatTime(), prevChat.getChatTime())) ? RIGHT_TEXT_SINGLE : RIGHT_TEXT_TAIL;
            }
        } else {
            ChatBubble prevChat = chatList.get(position - 1);
            ChatBubble nextChat = chatList.get(position + 1);

            if (!currentChat.getSender().equals(prevChat.getSender())) {
                if (!currentChat.getSender().equals(nextChat.getSender())) {
                    type = RIGHT_TEXT_SINGLE;
                } else {
                    type = (!equalShotTime(currentChat.getChatTime(), nextChat.getChatTime())) ? RIGHT_TEXT_SINGLE : RIGHT_TEXT_HEADER;
                }
            } else {
                if (!currentChat.getSender().equals(nextChat.getSender())) {
                    type = RIGHT_TEXT_TAIL;
                } else {
                    type = (!equalShotTime(currentChat.getChatTime(), nextChat.getChatTime())) ? RIGHT_TEXT_TAIL : RIGHT_TEXT_HEADER;
                }
            }
        }

        return convertChatType(currentChat.getChatType(), type);
    }

    public int getLeftChatBubbleType(int position) {
        /*
        채팅 타입 정리
        LEFT_SINGLE (LS) - 프로필 사진, 이름, 시간, 메시지 내용
        LEFT_HEADER (LH) - 프로필 사진, 이름, 메시지 내용
        LEFT_BODY (LB) - 메시지 내용
        LEFT_TAIL (LT) - 시간, 메시지 내용

        현재 채팅은 다른 사람이 보낸 채팅
        1) 가장 처음에 위치한 채팅 인경우
            1.0) 총 채팅이 1개인 경우 - LS
            1.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우 - LS
            1.2) 다음 채팅과 보낸 사람이 같은 경우
                1.1.1) 전송 시간이 다른 경우 - LS
                1.2.2) 전송 시간이 동일한 경우 - LH
        2) 가장 끝에 위치한 채팅 경우
            2.1) 이전 채팅이 다른 사람이 보낸 채팅인 경우 - LS
            2.2) 이전 채팅과 보낸 사람이 같은 경우
                2.2.1) 전송 시간이 다른 경우 - LS
                2.2.2) 전송 시간이 동일한 경우 - LT
        3) 중간에 위치한 채팅인 경우
            3.1) 이전 채팅이 다른 사람이 보낸 채팅인 경우
                3.1.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우 - LS
                3.1.2) 다음 채팅과 보낸 사람이 같은 경우
                    2.2.1) 전송 시간이 다른 경우 - LS
                    2.2.2) 전송 시간이 동일한 경우 - LH
            3.2) 이전 채팅과 보낸 사람이 같은 경우
                3.2.1) 다음 채팅이 다른 사람이 보낸 채팅인 경우 - LT
                3.2.2) 다음 채팅과 보낸 사람이 같은 경우
                    2.2.1) 이전 채팅과 시간이 다르고 다음 채팅과 시간이 다른 경우 - LS
                    2.2.2) 이전 채팅과 시간이 다르고 다음 채팅과 시간이 같은 경우 - LH
                    2.2.3) 이전 채팅과 시간이 같고 다음 채팅과 시간이 다른 경우 - LT
                    2.2.4) 이전 채팅과 시간이 같고 다음 채팅과 시간도 같은 경우 - LB
         */

        ChatBubbleType type = INVALID;
        ChatBubble currentChat = chatList.get(position);

        if (position == 0) {
            if (getItemCount() == 1) {
                type = LEFT_TEXT_SINGLE;
            } else {
                ChatBubble nextChat = chatList.get(position + 1);

                if (!currentChat.getSender().equals(nextChat.getSender())) {
                    type = LEFT_TEXT_SINGLE;
                } else {
                    type = (!equalShotTime(currentChat.getChatTime(), nextChat.getChatTime())) ? LEFT_TEXT_SINGLE : LEFT_TEXT_HEADER;
                }
            }
        } else if (position == getItemCount() - 1) {
            ChatBubble prevChat = chatList.get(position - 1);

            if (!currentChat.getSender().equals(prevChat.getSender())) {
                type = LEFT_TEXT_SINGLE;
            } else {
                type = (!equalShotTime(currentChat.getChatTime(), prevChat.getChatTime())) ? LEFT_TEXT_SINGLE : LEFT_TEXT_TAIL;
            }
        } else {
            ChatBubble prevChat = chatList.get(position - 1);
            ChatBubble nextChat = chatList.get(position + 1);

            if (!currentChat.getSender().equals(prevChat.getSender())) {
                if (!currentChat.getSender().equals(nextChat.getSender())) {
                    type = LEFT_TEXT_SINGLE;
                } else {
                    type = (!equalShotTime(currentChat.getChatTime(), nextChat.getChatTime())) ? LEFT_TEXT_SINGLE : LEFT_TEXT_HEADER;
                }
            } else {
                if (!currentChat.getSender().equals(nextChat.getSender())) {
                    type = LEFT_TEXT_TAIL;
                } else {
                    if (!equalShotTime(currentChat.getChatTime(), prevChat.getChatTime())) {
                        type = (!equalShotTime(currentChat.getChatTime(), nextChat.getChatTime())) ? LEFT_TEXT_SINGLE : LEFT_TEXT_HEADER;
                    } else {
                        type = (!equalShotTime(currentChat.getChatTime(), nextChat.getChatTime())) ? LEFT_TEXT_TAIL : LEFT_TEXT_BODY;
                    }
                }
            }
        }

        return convertChatType(currentChat.getChatType(), type);
    }

    public int convertChatType(int contentType, ChatBubbleType chatBubbleType) {
        int type;

        switch(contentType) {
            case CHAT_TYPE_TEXT:
                type = chatBubbleType.getType();
                break;
            case CHAT_TYPE_IMAGE:
                type = chatBubbleType.getType() + 1;
                break;
            case CHAT_TYPE_FILE:
                type = chatBubbleType.getType() + 2;
                break;
            case CHAT_TYPE_AUDIO:
                type = chatBubbleType.getType() + 3;
                break;
            default:
                type = CHAT_TYPE_INVALID;
                break;
        }

        return type;
    }

    public boolean equalShotTime(String time1, String time2) {
        return getShortTime(time1).equals(getShortTime(time2));
    }

    public static String getShortTime(String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(time, formatter);
        return dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
    }

    public void startProfileActivity(View view, Member member) {
        Intent intent = new Intent(view.getContext(), ProfileActivity.class);
        intent.putExtra("memberId", String.valueOf(member.getId()));

        view.getContext().startActivity(intent);
    }

    // left
    public class ChatBubbleLeftTextViewHolder extends RecyclerView.ViewHolder {
        ImageView senderImage;
        TextView chatMessage;
        TextView chatTime;
        TextView chatSender;
        int chatType;

        public ChatBubbleLeftTextViewHolder(@NonNull View itemView, int chatType) {
            super(itemView);
            senderImage = itemView.findViewById(R.id.message_sender_image);
            chatMessage = itemView.findViewById(R.id.left_chat_text);
            chatSender = itemView.findViewById(R.id.message_sender);
            chatTime = itemView.findViewById(R.id.left_chat_time);
            this.chatType = chatType;
        }

        public void setUserClickEvent(Member member) {
            this.senderImage.setOnClickListener(v -> {
                startProfileActivity(v, member);
            });
        }

        public void setChatBubble(ChatBubble chatBubble) {
            Member member = getChatSendMember(chatBubble.getSender());
            setProfileImage(senderImage, member.getProfileImage());

            setUserClickEvent(member);
            chatSender.setText(member.getUsername());
            chatMessage.setText(chatBubble.getChatMsg());
            chatTime.setText(getShortTime(chatBubble.getChatTime()));

            if(chatType == LEFT_TEXT_HEADER.getType()) {
                chatTime.setVisibility(View.INVISIBLE);
            }
        }
    }

    public static class ChatBubbleLeftTextOnlyViewHolder extends RecyclerView.ViewHolder {
        TextView chatMessage;
        TextView chatTime;
        int chatType;

        public ChatBubbleLeftTextOnlyViewHolder(@NonNull View itemView, int chatType) {
            super(itemView);
            chatMessage = itemView.findViewById(R.id.left_chat_text);
            chatTime = itemView.findViewById(R.id.left_chat_time);
            this.chatType = chatType;
        }

        public void setChatBubble(ChatBubble chatBubble) {
            chatMessage.setText(chatBubble.getChatMsg());
            chatTime.setText(getShortTime(chatBubble.getChatTime()));

            if(chatType == LEFT_TEXT_BODY.getType()) {
                chatTime.setVisibility(View.INVISIBLE);
            }
        }
    }

    public class ChatBubbleLeftImageViewHolder extends RecyclerView.ViewHolder {
        ImageView senderImage;
        ImageView chatImage;
        TextView chatTime;
        TextView chatSender;
        int chatType;

        public ChatBubbleLeftImageViewHolder(@NonNull View itemView, int chatType) {
            super(itemView);
            senderImage = itemView.findViewById(R.id.message_sender_image);
            chatImage = itemView.findViewById(R.id.left_chat_image);
            chatSender = itemView.findViewById(R.id.message_sender);
            chatTime = itemView.findViewById(R.id.left_chat_time);
            this.chatType = chatType;
        }

        public void setUserClickEvent(Member member) {
            this.senderImage.setOnClickListener(v -> {
                startProfileActivity(v, member);
            });
        }

        public void setChatImageClickEvent(ChatBubble chat) {
            this.chatImage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChatImageActivity.class);

                ArrayList<String> imageFileList = new ArrayList<>();
                imageFileList.add(chat.getChatMsg());
                intent.putStringArrayListExtra("imageFileList", imageFileList);

                v.getContext().startActivity(intent);
            });
        }

        public void setChatBubble(ChatBubble chatBubble) {
            Member member = getChatSendMember(chatBubble.getSender());

            setProfileImage(senderImage, member.getProfileImage());
            setProfileImage(chatImage, chatBubble.getChatMsg());

            setUserClickEvent(member);
            setChatImageClickEvent(chatBubble);
            chatSender.setText(member.getUsername());
            chatTime.setText(getShortTime(chatBubble.getChatTime()));

            if(chatType == LEFT_IMAGE_HEADER.getType()) {
                chatTime.setVisibility(View.INVISIBLE);
            }
        }
    }

    public static class ChatBubbleLeftImageOnlyViewHolder extends RecyclerView.ViewHolder {
        ImageView chatImage;
        TextView chatTime;
        int chatType;

        public ChatBubbleLeftImageOnlyViewHolder(@NonNull View itemView, int chatType) {
            super(itemView);
            chatImage = itemView.findViewById(R.id.left_chat_image);
            chatTime = itemView.findViewById(R.id.left_chat_time);
            this.chatType = chatType;
        }

        public void setChatImageClickEvent(ChatBubble chat) {
            this.chatImage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChatImageActivity.class);

                ArrayList<String> imageFileList = new ArrayList<>();
                imageFileList.add(chat.getChatMsg());
                intent.putStringArrayListExtra("imageFileList", imageFileList);

                v.getContext().startActivity(intent);
            });
        }

        public void setChatBubble(ChatBubble chatBubble) {
            setProfileImage(chatImage, chatBubble.getChatMsg());

            setChatImageClickEvent(chatBubble);
            chatTime.setText(getShortTime(chatBubble.getChatTime()));

            if(chatType == LEFT_IMAGE_BODY.getType()) {
                chatTime.setVisibility(View.INVISIBLE);
            }
        }
    }

    // right
    public static class ChatBubbleRightTextViewHolder extends RecyclerView.ViewHolder {
        TextView chatMessage;
        TextView chatTime;
        int chatType;

        public ChatBubbleRightTextViewHolder(@NonNull View itemView, int chatType) {
            super(itemView);
            chatMessage = itemView.findViewById(R.id.right_chat_text);
            chatTime = itemView.findViewById(R.id.right_chat_time);
            this.chatType = chatType;
        }

        public void setChatBubble(ChatBubble chatBubble) {
            chatMessage.setText(chatBubble.getChatMsg());
            chatTime.setText(getShortTime(chatBubble.getChatTime()));

            if(chatType == RIGHT_TEXT_HEADER.getType()) {
                chatTime.setVisibility(View.INVISIBLE);
            }
        }
    }

    public static class ChatBubbleRightImageViewHolder extends RecyclerView.ViewHolder {
        ImageView chatImage;
        TextView chatTime;
        int chatType;

        public ChatBubbleRightImageViewHolder(@NonNull View itemView, int chatType) {
            super(itemView);
            chatImage = itemView.findViewById(R.id.right_chat_image);
            chatTime = itemView.findViewById(R.id.right_chat_time);
            this.chatType = chatType;
        }

        public void setChatImageClickEvent(ChatBubble chat) {
            this.chatImage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ChatImageActivity.class);

                ArrayList<String> imageFileList = new ArrayList<>();
                imageFileList.add(chat.getChatMsg());
                intent.putStringArrayListExtra("imageFileList", imageFileList);

                v.getContext().startActivity(intent);
            });
        }

        public void setChatBubble(ChatBubble chatBubble) {
            setProfileImage(chatImage, chatBubble.getChatMsg());
            setChatImageClickEvent(chatBubble);
            chatTime.setText(getShortTime(chatBubble.getChatTime()));

            if(chatType == RIGHT_IMAGE_HEADER.getType()) {
                chatTime.setVisibility(View.INVISIBLE);
            }
        }
    }
}
