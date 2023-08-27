package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.R;

import java.util.List;

public class ChatImageSliderAdapter extends RecyclerView.Adapter<ChatImageSliderAdapter.ChatImageSliderViewHolder> {

    private final List<String> chatImages;

    public ChatImageSliderAdapter(List<String> imageChatList) {
        this.chatImages = imageChatList;
    }

    @NonNull
    @Override
    public ChatImageSliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.chat_image_item_slider, parent, false);
        return new ChatImageSliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatImageSliderViewHolder holder, int position) {
        holder.bindSliderImage(chatImages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatImages.size();
    }

    public static class ChatImageSliderViewHolder extends RecyclerView.ViewHolder {
        ImageView chatImage;

        public ChatImageSliderViewHolder(View view) {
            super(view);

            this.chatImage = view.findViewById(R.id.chat_image_slider);
        }

        public void bindSliderImage(String imageFile) {
            setProfileImage(this.chatImage, imageFile);
        }
    }
}
