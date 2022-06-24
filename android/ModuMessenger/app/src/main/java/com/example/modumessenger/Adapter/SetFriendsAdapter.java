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
import com.example.modumessenger.Activity.SetFriendsActivity;
import com.example.modumessenger.Grid.SettingGridItem;
import com.example.modumessenger.R;

import java.util.ArrayList;
import java.util.List;

public class SetFriendsAdapter extends RecyclerView.Adapter<SetFriendsAdapter.SetFriendsViewHolder> {

    List<String> setFriendsList = new ArrayList<>();

    public SetFriendsAdapter() {
        setFriendsList.add("즐겨찾기");
        setFriendsList.add("숨김친구");
        setFriendsList.add("차단친구");
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
        String setFriendName = this.setFriendsList.get(position);

        holder.setSettingName(setFriendName);
        holder.setUserClickEvent();
    }

    @Override
    public int getItemCount() {
        return setFriendsList.size();
    }

    public static class SetFriendsViewHolder extends RecyclerView.ViewHolder {
        TextView setting_name;
        ImageView setting_image;
        ConstraintLayout setting_card_view;

        public SetFriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            setting_name = itemView.findViewById(R.id.setting_friends_name);
            setting_image = itemView.findViewById(R.id.setting_friends_image);
            setting_card_view = itemView.findViewById(R.id.setFriendCardViewLayout);
        }

        public void setSettingName(String name) {
            this.setting_name.setText(name);

            int drawableImage = 0;

            switch (name) {
                case "즐겨찾기":
                    drawableImage = R.drawable.ic_baseline_person_24;
                    break;
                case "숨김친구":
                    drawableImage = R.drawable.ic_baseline_person_outline_24;
                    break;
                case "차단친구":
                    drawableImage = R.drawable.ic_baseline_person_off_24;
                    break;
                default:
                    drawableImage = R.drawable.basic_profile_image;
                    break;
            }

            Glide.with(setting_image)
                    .load(drawableImage)
                    .error(Glide.with(setting_image)
                            .load(R.drawable.basic_profile_image)
                            .into(setting_image))
                    .into(setting_image);
        }

        public void setUserClickEvent() {
            this.setting_card_view.setOnClickListener(v -> {
                String setName = (String) setting_name.getText();

                Intent intent = null;

                switch (setName) {
                    case "즐겨찾기":
                        intent = new Intent(v.getContext(), SetFriendsActivity.class);
                        break;
                    case "숨김친구":
                        intent = new Intent(v.getContext(), SetFriendsActivity.class);
                        break;
                    case "차단친구":
                        intent = new Intent(v.getContext(), SetFriendsActivity.class);
                        break;
                    default:
                        break;
                }

                if(intent!=null) {
                    v.getContext().startActivity(intent);
                }
            });
        }
    }
}