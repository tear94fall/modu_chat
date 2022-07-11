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
import com.example.modumessenger.R;

import java.util.ArrayList;
import java.util.List;

public class SendOthersGridAdapter extends BaseAdapter {
    private final Context context;
    private final List<SendOthersGridItem> sendOthersGridItemList;

    public SendOthersGridAdapter(Context context) {
        this.context = context;
        this.sendOthersGridItemList = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return this.sendOthersGridItemList.size();
    }

    @Override
    public Object getItem(int position) {
        return this.sendOthersGridItemList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.send_others_grid_item, parent, false);

        SendOthersGridItem sendOthersGridItem = sendOthersGridItemList.get(position);

        TextView itemTextView = convertView.findViewById(R.id.send_others_text_view);
        itemTextView.setText(sendOthersGridItem.getItemName());

        ImageView itemImageView = convertView.findViewById(R.id.send_others_image_view);
        Glide.with(this.context)
                .load(sendOthersGridItem.getItemImage())
                .error(Glide.with(this.context)
                        .load(R.drawable.basic_profile_image)
                        .into(itemImageView))
                .into(itemImageView);

        return convertView;
    }

    public void setGridItems(View view) {
        setGridItem(new SendOthersGridItem("사진 전송", R.drawable.ic_baseline_image_24));
        setGridItem(new SendOthersGridItem("파일 전송", R.drawable.ic_baseline_attach_file_24));
        setGridItem(new SendOthersGridItem("음성 전송", R.drawable.ic_baseline_audio_file_24));
    }

    public void setGridItem(SendOthersGridItem sendOthersGridItem) {
        this.sendOthersGridItemList.add(sendOthersGridItem);
    }

    public List<SendOthersGridItem> getGridItems() {
        return this.sendOthersGridItemList;
    }

    public SendOthersGridItem getGridItem(int index) {
        return this.sendOthersGridItemList.get(index);
    }
}
