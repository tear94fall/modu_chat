package com.example.modumessenger.Adapter;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.R;
import com.example.modumessenger.entity.CommonData;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final SparseBooleanArray selectedItems = new SparseBooleanArray();
    private final List<CommonData> notificationList;
    private Context context;
    private int prePosition = -1;

    public NotificationAdapter(List<CommonData> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationAdapter.NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.notification_row, parent, false);
        context = parent.getContext();
        return new NotificationAdapter.NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationAdapter.NotificationViewHolder holder, int position) {
        CommonData commonData = this.notificationList.get(position);

        holder.setNotification(commonData);
        holder.changeSpreadContent(position);
        holder.setClickEvent(commonData);
    }

    @Override
    public int getItemCount() {
        return this.notificationList.size();
    }

    void addItem(CommonData commonData) {
        this.notificationList.add(commonData);
    }

    public class NotificationViewHolder extends RecyclerView.ViewHolder {

        TextView notificationDate;
        TextView notificationTitle;
        TextView notificationContent;
        ImageView notificationImage;
        Button spreadButton;
        ConstraintLayout detailLayout;
        int position;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationDate = itemView.findViewById(R.id.notification_date);
            notificationTitle = itemView.findViewById(R.id.notification_title);
            notificationContent = itemView.findViewById(R.id.notification_detail_content);
            notificationImage = itemView.findViewById(R.id.notification_detail_image);
            spreadButton = itemView.findViewById(R.id.notification_spread_button);
            detailLayout = itemView.findViewById(R.id.notification_detail_layout);
        }

        public void changeSpreadContent(int position) {
            this.changeVisibility(selectedItems.get(position));
        }

        public void setNotification(CommonData commonData) {
            this.notificationDate.setText(commonData.getKey());
            this.notificationTitle.setText(commonData.getValue());
            this.notificationContent.setText(commonData.getValue());
            this.notificationImage.setImageResource(R.drawable.modu_banner);
        }

        public void setClickEvent(CommonData commonData) {
            spreadButton.setOnClickListener(v -> {
                // spread notification content
                if (selectedItems.get(position)) {
                    selectedItems.delete(position);
                } else {
                    selectedItems.delete(prePosition);
                    selectedItems.put(position, true);
                }
                if (prePosition != -1) notifyItemChanged(prePosition);
                notifyItemChanged(position);
                prePosition = position;
            });
        }

        private void changeVisibility(final boolean isExpanded) {
            int dpValue = 200;
            DisplayMetrics metrics = context.getResources().getDisplayMetrics();
            float d = metrics.density;
            int height = (int) (dpValue * d);

            ValueAnimator valueAnimator = isExpanded ? ValueAnimator.ofInt(0, height) : ValueAnimator.ofInt(height, 0);
            valueAnimator.setDuration(600);
            valueAnimator.addUpdateListener(animation -> {
                detailLayout.getLayoutParams().height = (int) animation.getAnimatedValue();
                detailLayout.requestLayout();
                detailLayout.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            });

            valueAnimator.start();
        }
    }
}
