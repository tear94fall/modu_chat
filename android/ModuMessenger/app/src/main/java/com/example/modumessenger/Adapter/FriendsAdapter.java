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
import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.R;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.MyViewHolder> {

    String[] username;
    String[] statusMessage;
    String[] profileImage;

    public FriendsAdapter(String[] username, String[] statusMessage, String[] profileImage) {
        this.username = username;
        this.statusMessage = statusMessage;
        this.profileImage = profileImage;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.friend_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.textView1.setText(username[position]);
        holder.textView2.setText(statusMessage[position]);
        Glide.with(holder.imageView).load(profileImage[position]).into(holder.imageView);

        holder.cardViewLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("username", username[position]);
                intent.putExtra("statusMessage", statusMessage[position]);
                intent.putExtra("profileImage", profileImage[position]);

                v.getContext().startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return profileImage.length;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {

        TextView textView1, textView2;
        ImageView imageView;
        ConstraintLayout cardViewLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            textView1 = itemView.findViewById(R.id.textView);
            textView2 = itemView.findViewById(R.id.textView4);
            imageView = itemView.findViewById(R.id.imageView2);
            cardViewLayout = itemView.findViewById(R.id.cardViewLayout);
        }
    }
}
