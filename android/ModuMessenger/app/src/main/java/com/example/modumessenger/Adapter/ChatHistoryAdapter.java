package com.example.modumessenger.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
            ((ChatBubbleLeftViewHolder) holder).textView.setText(chatList.get(position).getChatMsg());
        } else if (holder instanceof  ChatBubbleRightViewHolder) {
            ((ChatBubbleRightViewHolder) holder).textView.setText(chatList.get(position).getChatMsg());
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
        TextView textView;

        public ChatBubbleLeftViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.left_chat_text);
        }
    }

    public static class ChatBubbleRightViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ChatBubbleRightViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(R.id.right_chat_text);
        }
    }
}
