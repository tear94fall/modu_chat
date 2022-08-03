package com.example.modumessenger.Grid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Activity.ChatSendOthersActivity;
import com.example.modumessenger.Adapter.ChatBubble;
import com.example.modumessenger.R;
import com.example.modumessenger.dto.ChatDto;

import java.util.ArrayList;
import java.util.List;

public class RecentChatImageGridAdapter extends BaseAdapter {
    private final Context context;
    private final List<RecentChatImageGridItem> recentChatImageGridItems;

    public RecentChatImageGridAdapter(Context context) {
        this.context = context;
        this.recentChatImageGridItems = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return this.recentChatImageGridItems.size();
    }

    @Override
    public Object getItem(int position) {
        return this.recentChatImageGridItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.recent_chat_image_grid_item, parent, false);

        RecentChatImageGridItem recentChatImageGridItem = recentChatImageGridItems.get(position);

        ImageView itemImageView = convertView.findViewById(R.id.recent_chat_image_view);
        Glide.with(this.context)
                .load(recentChatImageGridItem.getImageUrl())
                .error(Glide.with(this.context)
                        .load(R.drawable.basic_profile_image)
                        .into(itemImageView))
                .into(itemImageView);

        return convertView;
    }

    public void setGridItems(List<ChatDto> chatDtoList) {
        chatDtoList.forEach(chatDto -> setGridItem(new RecentChatImageGridItem(chatDto.getMessage())));
    }

    public void setGridItem(RecentChatImageGridItem recentChatImageGridItem) {
        this.recentChatImageGridItems.add(recentChatImageGridItem);
    }

    public List<RecentChatImageGridItem> getGridItems() {
        return this.recentChatImageGridItems;
    }

    public RecentChatImageGridItem getGridItem(int index) {
        return this.recentChatImageGridItems.get(index);
    }
}
