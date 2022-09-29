package com.example.modumessenger.Adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Activity.AppInfoActivity;
import com.example.modumessenger.Activity.NotificationActivity;
import com.example.modumessenger.Activity.SetAccountActivity;
import com.example.modumessenger.Activity.SetFriendsActivity;
import com.example.modumessenger.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetUpAdapter extends RecyclerView.Adapter<SetUpAdapter.SetUpViewHolder> {

    List<SetUp> setUpList = new ArrayList<>();

    public SetUpAdapter() {
        setSetUpItems();
    }

    @NonNull
    @Override
    public SetUpAdapter.SetUpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.set_up_row, parent, false);
        return new SetUpAdapter.SetUpViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SetUpAdapter.SetUpViewHolder holder, int position) {
        SetUp setUp = this.setUpList.get(position);

        holder.setSettingName(setUp);
        holder.setUserClickEvent(setUp);
    }

    @Override
    public int getItemCount() {
        return setUpList.size();
    }

    public void setSetUpItems() {
        setUpList.add(SetUp.SET_UP_ACCOUNT);
        setUpList.add(SetUp.SET_UP_APP_VERSION_INFO);
        setUpList.add(SetUp.SET_UP_NOTIFICATION);
        setUpList.add(SetUp.SET_UP_FRIENDS_SETTING);
    }

    public static class SetUpViewHolder extends RecyclerView.ViewHolder {
        TextView set_up_name;
        ImageView set_up_image;
        ConstraintLayout set_up_card_view;

        Map<String, Intent> setUpMap = new HashMap<>();

        public SetUpViewHolder(@NonNull View itemView) {
            super(itemView);

            setSetUpViews(itemView);

            set_up_name = itemView.findViewById(R.id.set_up_name);
            set_up_image = itemView.findViewById(R.id.set_up_image);
            set_up_card_view = itemView.findViewById(R.id.setUpCardViewLayout);
        }

        public void setSetUpViews(View view) {
            setUpMap.put(SetUp.SET_UP_ACCOUNT.getSetUpName(), new Intent(view.getContext(), SetAccountActivity.class));
            setUpMap.put(SetUp.SET_UP_APP_VERSION_INFO.getSetUpName(), new Intent(view.getContext(), AppInfoActivity.class));
            setUpMap.put(SetUp.SET_UP_NOTIFICATION.getSetUpName(), new Intent(view.getContext(), NotificationActivity.class));
            setUpMap.put(SetUp.SET_UP_FRIENDS_SETTING.getSetUpName(), new Intent(view.getContext(), SetFriendsActivity.class));
        }

        public void setSettingName(SetUp setUp) {
            this.set_up_name.setText(setUp.getSetUpName());

            Glide.with(set_up_image)
                    .load(setUp.getSetUpImage())
                    .error(Glide.with(set_up_image)
                            .load(R.drawable.basic_profile_image)
                            .into(set_up_image))
                    .into(set_up_image);
        }

        public void setUserClickEvent(SetUp setUp) {
            this.set_up_card_view.setOnClickListener(view -> {
                Intent intent = setUpMap.get(setUp.getSetUpName());
                if(intent!=null) {
                    view.getContext().startActivity(intent);
                }
            });
        }
    }

    public enum SetUp {
        SET_UP_ACCOUNT("계정설정", R.drawable.ic_baseline_account_circle_24),
        SET_UP_APP_VERSION_INFO("버전정보", R.drawable.ic_baseline_info_24),
        SET_UP_NOTIFICATION("공지사항", R.drawable.ic_baseline_celebration_24),
        SET_UP_FRIENDS_SETTING("친구설정", R.drawable.ic_baseline_person_24);

        private final String setUpName;
        private final int itemImage;

        SetUp(String setUpName, int itemImage) {
            this.setUpName = setUpName;
            this.itemImage = itemImage;
        }

        public String getSetUpName() {
            return setUpName;
        }

        public int getSetUpImage() {
            return itemImage;
        }
    }
}