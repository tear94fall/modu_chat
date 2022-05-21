package com.example.modumessenger.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Activity.ChatActivity;
import com.example.modumessenger.R;
import com.example.modumessenger.dto.ChatBubbleViewType;

import java.util.ArrayList;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ChatBubble> chatList;

    public ChatHistoryAdapter(List<ChatBubble> chatList) {
        this.chatList = (chatList == null || chatList.size() == 0) ? new ArrayList<>() : chatList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        if(viewType == ChatBubbleViewType.LEFT) {
            view = inflater.inflate(R.layout.chat_bubble_left, parent, false);
            return new ChatHistoryAdapter.ChatBubbleLeftViewHolder(view);
        } else if(viewType == ChatBubbleViewType.RIGHT) {
            view = inflater.inflate(R.layout.chat_bubble_right, parent, false);
            return new ChatHistoryAdapter.ChatBubbleRightViewHolder(view);
        } else {
            view = inflater.inflate(R.layout.chat_bubble_right, parent, false);
            return new ChatHistoryAdapter.ChatBubbleRightViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if(holder instanceof ChatBubbleLeftViewHolder) {

            Glide.with(((ChatBubbleLeftViewHolder) holder).senderImage)
                    .load(R.drawable.basic_profile_image)
                    .into(((ChatBubbleLeftViewHolder) holder).senderImage);
//            Glide.with(((ChatBubbleLeftViewHolder) holder).senderImage)
//                    .load(chatList.get(position).getSender())
//                    .error(Glide.with(((ChatBubbleLeftViewHolder) holder).senderImage)
//                            .load(R.drawable.basic_profile_image)
//                            .into(((ChatBubbleLeftViewHolder) holder).senderImage))
//                    .into(((ChatBubbleLeftViewHolder) holder).senderImage);

            ((ChatBubbleLeftViewHolder) holder).chatSender.setText(chatList.get(position).getSender());
            ((ChatBubbleLeftViewHolder) holder).chatMessage.setText(chatList.get(position).getChatMsg());
            ((ChatBubbleLeftViewHolder) holder).leftChatTime.setText(chatList.get(position).getChatTime());
        } else if (holder instanceof  ChatBubbleRightViewHolder) {
            ((ChatBubbleRightViewHolder) holder).chatMessage.setText(chatList.get(position).getChatMsg());
            ((ChatBubbleRightViewHolder) holder).rightChatTime.setText(chatList.get(position).getChatTime());
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        return chatList.get(position).getViewType();
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
