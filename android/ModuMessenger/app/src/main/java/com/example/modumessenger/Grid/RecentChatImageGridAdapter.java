package com.example.modumessenger.Grid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.modumessenger.Activity.ChatImageActivity;
import com.example.modumessenger.Global.DataStoreHelper;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class RecentChatImageGridAdapter extends BaseAdapter {
    private final Context context;
    private final List<RecentChatImageGridItem> recentChatImageGridItems;
    private ArrayList<String> recentImageList;

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

        itemImageView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ChatImageActivity.class);
            intent.putStringArrayListExtra("chatImageList", new ArrayList<>(Collections.singletonList(recentImageList.get(position))));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            v.getContext().startActivity(intent);
        });

        String accessToken = DataStoreHelper.getDataStoreStr("access-token");
        String url = RetrofitClient.getBaseUrl() + "storage-service/view/"+ recentChatImageGridItem.getImageUrl();

        GlideUrl glideUrl = new GlideUrl(url,
                new LazyHeaders.Builder()
                        .addHeader("Authorization", accessToken)
                        .build());

        Glide.with(this.context)
                .load(glideUrl)
                .error(Glide.with(this.context)
                        .load(R.drawable.basic_profile_image)
                        .into(itemImageView))
                .into(itemImageView);

        return convertView;
    }

    public void setRecentImageList(ArrayList<String> recentImageList) {
        this.recentImageList = recentImageList;
    }

    public void setGridItems(List<ChatDto> chatDtoList) {
        List<ChatDto> displayChats = chatDtoList.stream().limit(3).collect(Collectors.toList());
        displayChats.forEach(chatDto -> setGridItem(new RecentChatImageGridItem(chatDto.getMessage())));
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
