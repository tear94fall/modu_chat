package com.example.modumessenger.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.R;
import com.example.modumessenger.dto.NotificationDto;
import com.example.modumessenger.entity.CommonData;

import java.util.List;
import java.util.stream.Collectors;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final List<NotificationDto> notificationList;

    public NotificationAdapter(List<CommonData> notificationList) {
        this.notificationList = notificationList.stream()
                .map(commonData -> new NotificationDto(commonData.getKey(), commonData.getValue()))
                .collect(Collectors.toList());
    }

    @NonNull
    @Override
    public NotificationAdapter.NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.notification_row, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationAdapter.NotificationViewHolder holder, int position) {
        NotificationDto notificationDto = this.notificationList.get(position);

        holder.setNotification(notificationDto);
        holder.setContentHide(notificationDto);
        holder.setClickEvent(notificationDto);
    }

    @Override
    public int getItemCount() {
        return this.notificationList.size();
    }

    void addItem(NotificationDto notificationDto) {
        this.notificationList.add(notificationDto);
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView notificationDate;
        TextView notificationTitle;
        TextView notificationContent;
        ImageView notificationImage;
        ImageButton spreadImageButton;
        ConstraintLayout detailLayout;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            notificationDate = itemView.findViewById(R.id.notification_date);
            notificationTitle = itemView.findViewById(R.id.notification_title);
            notificationContent = itemView.findViewById(R.id.notification_detail_content);
            notificationImage = itemView.findViewById(R.id.notification_detail_image);
            spreadImageButton = itemView.findViewById(R.id.notification_spread_image_button);
            detailLayout = itemView.findViewById(R.id.notification_detail_layout);
        }

        public void setNotification(NotificationDto notificationDto) {
            this.notificationDate.setText(notificationDto.getKey());
            this.notificationTitle.setText(notificationDto.getValue());
            this.notificationContent.setText(notificationDto.getValue());
            this.notificationImage.setImageResource(R.drawable.modu_banner);
        }

        public void setContentHide(NotificationDto notificationDto) {
            ContentAnimation.hideContent(this.detailLayout);
            notificationDto.setExpanded(false);
        }

        public void setClickEvent(NotificationDto notificationDto) {
            spreadImageButton.setOnClickListener(v -> {
                boolean currentStatus = notificationDto.getExpanded();
                contentButtonClick(!currentStatus, v, detailLayout);
                notificationDto.setExpanded(!currentStatus);
            });
        }

        public void contentButtonClick(boolean isExpanded, View view, ConstraintLayout constraintLayout) {
            ContentAnimation.rotateButton(view, isExpanded);
            ContentAnimation.actionContentAnimation(constraintLayout, isExpanded);
        }
    }

    public static class ContentAnimation {
        public static void rotateButton(View view, boolean isExpanded) {
            view.animate().setDuration(200).rotation(isExpanded ? 180f : 0f);
        }

        public static void actionContentAnimation(ConstraintLayout constraintLayout, boolean isExpanded) {
            if(isExpanded) {
                showContent(constraintLayout);
            } else {
                hideContent(constraintLayout);
            }
        }

        public static void showContent(ConstraintLayout view) {
            view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int actualHeight = view.getMeasuredHeight();
            view.getLayoutParams().height = 0;
            view.setVisibility(View.VISIBLE);

            Animation animation = new Animation() {
                @Override
                public void applyTransformation(float interpolatedTime, Transformation t) {
                    view.getLayoutParams().height = (interpolatedTime == 1f) ? ViewGroup.LayoutParams.WRAP_CONTENT : (int) (actualHeight * interpolatedTime);
                    view.requestLayout();
                }
            };

            animation.setDuration((long) (actualHeight / view.getContext().getResources().getDisplayMetrics().density));
            view.startAnimation(animation);
        }

        public static void hideContent(ConstraintLayout view) {
            int actualHeight = view.getMeasuredHeight();

            Animation animation = new Animation() {
                @Override
                public void applyTransformation(float interpolatedTime, Transformation t) {
                    if (interpolatedTime == 1f) {
                        view.setVisibility(View.GONE);
                    } else {
                        view.getLayoutParams().height = (int) (actualHeight - (actualHeight * interpolatedTime));
                        view.requestLayout();
                    }
                }
            };

            animation.setDuration((long) (actualHeight / view.getContext().getResources().getDisplayMetrics().density));
            view.startAnimation(animation);
        }
    }
}
