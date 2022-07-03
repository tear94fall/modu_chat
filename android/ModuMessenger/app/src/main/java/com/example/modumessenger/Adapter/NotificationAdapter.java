package com.example.modumessenger.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.R;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.CommonData;
import com.google.android.gms.common.internal.service.Common;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    List<CommonData> notificationList;

    public NotificationAdapter(List<CommonData> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationAdapter.NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.friend_row, parent, false);
        return new NotificationAdapter.NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationAdapter.NotificationViewHolder holder, int position) {
        CommonData commonData = this.notificationList.get(position);

        holder.setUserInfo(commonData);
        holder.setUserClickEvent(commonData);
    }

    @Override
    public int getItemCount() {
        return this.notificationList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {

        TextView notificationDate;
        TextView notificationTitle;
        Button spreadButton;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationDate = itemView.findViewById(R.id.notification_date);
            notificationTitle = itemView.findViewById(R.id.notification_title);
            spreadButton = itemView.findViewById(R.id.notification_spread_button);
        }

        public void setUserInfo(CommonData commonData) {
            this.notificationDate.setText(commonData.getKey());
            this.notificationTitle.setText(commonData.getValue());
        }

        public void setUserClickEvent(CommonData commonData) {
            spreadButton.setOnClickListener(v -> {
                // spread notification content
            });
        }
    }
}
