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
import com.example.modumessenger.Activity.SetFriendsActivity;
import com.example.modumessenger.Grid.SettingGridItem;
import com.example.modumessenger.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SetFriendsAdapter extends RecyclerView.Adapter<SetFriendsAdapter.SetFriendsViewHolder> {

    List<FriendSetUp> setFriendsList = new ArrayList<>();

    public SetFriendsAdapter() {
        setFriendSetUpItems();
    }

    @NonNull
    @Override
    public SetFriendsAdapter.SetFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.set_friends_row, parent, false);
        return new SetFriendsAdapter.SetFriendsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SetFriendsAdapter.SetFriendsViewHolder holder, int position) {
        FriendSetUp friendSetUp = this.setFriendsList.get(position);
        holder.setSettingName(friendSetUp);
        holder.setUserClickEvent(friendSetUp);
    }

    @Override
    public int getItemCount() {
        return setFriendsList.size();
    }

    public void setFriendSetUpItems() {
        setFriendsList.add(FriendSetUp.SET_UP_FRIENDS_FAVORITE);
        setFriendsList.add(FriendSetUp.SET_UP_FRIENDS_HIDDEN);
        setFriendsList.add(FriendSetUp.SET_UP_FRIENDS_BLOCKED);
    }

    public static class SetFriendsViewHolder extends RecyclerView.ViewHolder {
        TextView setting_name;
        ImageView setting_image;
        ConstraintLayout setting_card_view;

        Map<String, Intent> setUpMap = new HashMap<>();

        public SetFriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            setSetUpViews(itemView);

            setting_name = itemView.findViewById(R.id.setting_friends_name);
            setting_image = itemView.findViewById(R.id.setting_friends_image);
            setting_card_view = itemView.findViewById(R.id.setFriendCardViewLayout);
        }

        public void setSetUpViews(View view) {
            setUpMap.put(FriendSetUp.SET_UP_FRIENDS_FAVORITE.getSetUpName(), new Intent(view.getContext(), SetFriendsActivity.class));
            setUpMap.put(FriendSetUp.SET_UP_FRIENDS_HIDDEN.getSetUpName(), new Intent(view.getContext(), SetFriendsActivity.class));
            setUpMap.put(FriendSetUp.SET_UP_FRIENDS_BLOCKED.getSetUpName(), new Intent(view.getContext(), SetFriendsActivity.class));
        }

        public void setSettingName(FriendSetUp friendSetUp ) {
            this.setting_name.setText(friendSetUp.getSetUpName());

            Glide.with(setting_image)
                    .load(friendSetUp.getSetUpImage())
                    .error(Glide.with(setting_image)
                            .load(R.drawable.basic_profile_image)
                            .into(setting_image))
                    .into(setting_image);
        }

        public void setUserClickEvent(FriendSetUp friendSetUp) {
            this.setting_card_view.setOnClickListener(v -> {
                Intent intent = setUpMap.get(friendSetUp.getSetUpName());
                if(intent!=null) {
                    v.getContext().startActivity(intent);
                }
            });
        }
    }

    public enum FriendSetUp {
        SET_UP_FRIENDS_FAVORITE("즐겨찾기", R.drawable.ic_baseline_person_24),
        SET_UP_FRIENDS_HIDDEN("숨김친구", R.drawable.ic_baseline_person_outline_24),
        SET_UP_FRIENDS_BLOCKED("차단친구", R.drawable.ic_baseline_person_off_24);

        private final String setUpName;
        private final int itemImage;

        FriendSetUp(String setUpName, int itemImage) {
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