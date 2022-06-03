package com.example.modumessenger.Adapter;

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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.Member;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public final int LEFT = 1;
    public final int RIGHT = 2;
    public final int LEFT_DUP = 3;

    private final List<Member> memberList;
    private final List<ChatBubble> chatList;
    private String lastChatTime;
    private String userId;

    public ChatHistoryAdapter(List<ChatBubble> chatList, List<Member> memberList) {
        this.memberList = (memberList == null || memberList.size() == 0) ? new ArrayList<>() : memberList;
        this.chatList = (chatList == null || chatList.size() == 0) ? new ArrayList<>() : chatList;
        userId = PreferenceManager.getString("userId");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        if (viewType == LEFT) {
            view = inflater.inflate(R.layout.chat_bubble_left, parent, false);
            return new ChatHistoryAdapter.ChatBubbleLeftViewHolder(view);
        } else if (viewType == RIGHT) {
            view = inflater.inflate(R.layout.chat_bubble_right, parent, false);
            return new ChatHistoryAdapter.ChatBubbleRightViewHolder(view);
        } else if (viewType == LEFT_DUP) {
            view = inflater.inflate(R.layout.chat_bubble_left_dup, parent, false);
            return new ChatHistoryAdapter.ChatBubbleLeftDuplicateViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.chat_bubble_right, parent, false);
            return new ChatHistoryAdapter.ChatBubbleRightViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatBubble chat = chatList.get(position);

        if (holder instanceof ChatBubbleLeftViewHolder) {
            ChatBubbleLeftViewHolder leftHolder = ((ChatBubbleLeftViewHolder) holder);

            memberList.forEach(member->{
                if (member.getUserId().equals(chatList.get(position).getSender())) {

                    Glide.with(((ChatBubbleLeftViewHolder) holder).senderImage)
                            .load(member.getProfileImage())
                            .override(70, 70)
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                            .error(Glide.with(((ChatBubbleLeftViewHolder) holder).senderImage)
                                    .load(R.drawable.basic_profile_image)
                                    .into(((ChatBubbleLeftViewHolder) holder).senderImage))
                            .into(((ChatBubbleLeftViewHolder) holder).senderImage);

                    ((ChatBubbleLeftViewHolder) holder).setUserClickEvent(member);
                }
            });

            leftHolder.chatSender.setText(chat.getSender());
            leftHolder.chatMessage.setText(chat.getChatMsg());
            leftHolder.leftChatTime.setText(chat.getChatTime());
        } else if (holder instanceof ChatBubbleLeftDuplicateViewHolder) {
            ChatBubbleLeftDuplicateViewHolder leftDupHolder = ((ChatBubbleLeftDuplicateViewHolder) holder);
            leftDupHolder.chatMessage.setText(chat.getChatMsg());
            leftDupHolder.leftChatTime.setText(chat.getChatTime());
        } else if (holder instanceof  ChatBubbleRightViewHolder) {
            ChatBubbleRightViewHolder rightHolder = ((ChatBubbleRightViewHolder) holder);
            rightHolder.chatMessage.setText(chat.getChatMsg());
            rightHolder.rightChatTime.setText(chat.getChatTime());
        }

        lastChatTime = chat.getChatTime();
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatBubble chat = chatList.get(position);
        int type = RIGHT;

        if(!chat.getSender().equals(userId)){
            type = LEFT;
            if(position < getItemCount()-1 && position > 0) {
                ChatBubble beforeChat = chatList.get(position+1);
                ChatBubble afterChat = chatList.get(position-1);
                if(!chat.getSender().equals(beforeChat.getSender()) && chat.getSender().equals(afterChat.getSender())) {
                    type = LEFT_DUP;
                }
            } else if (position == getItemCount()-1) {
                ChatBubble afterChat = chatList.get(position-1);
                if(chat.getSender().equals(afterChat.getSender())) {
                    type = LEFT_DUP;
                }
            }
        }

        return type;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addChatMsg(ChatBubble chatBubble) {
        this.chatList.add(chatBubble);
        notifyDataSetChanged();
    }

    public static class ChatBubbleLeftViewHolder extends RecyclerView.ViewHolder {
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
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("username", member.getUsername());
                intent.putExtra("statusMessage", member.getStatusMessage());
                intent.putExtra("profileImage", member.getProfileImage());

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
}
