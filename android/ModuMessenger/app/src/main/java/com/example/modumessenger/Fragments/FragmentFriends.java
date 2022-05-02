package com.example.modumessenger.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.Adapter.FriendsAdapter;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;

public class FragmentFriends extends Fragment {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    // friend count
    TextView friendsCount;

    // my profile
    ConstraintLayout myProfileCard;
    TextView myName;
    TextView myStatusMessage;
    ImageView myProfileImage;

    String[] username;
    String[] statusMessage;
    String[] profileImage;

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.friend_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getActivity());

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.scrollToPosition(0);

        username = getResources().getStringArray(R.array.friend_name);
        statusMessage = getResources().getStringArray(R.array.description);
        profileImage = getResources().getStringArray(R.array.images);

        FriendsAdapter myAdapter = new FriendsAdapter(username, statusMessage, profileImage);
        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(layoutManager);

        // friend count
        friendsCount = view.findViewById(R.id.friendCount);
        String count = Integer.toString(myAdapter.getItemCount());
        String friendCountMessage = "친구 " + count + " 명";
        friendsCount.setText(friendCountMessage);

        // my profile
        myProfileCard = view.findViewById(R.id.myProfileCard);
        myProfileImage = view.findViewById(R.id.myProfileImage);
        myName = view.findViewById(R.id.myName);
        myStatusMessage = view.findViewById(R.id.myStatusMessage);

        myName.setText(PreferenceManager.getString("username"));
        myStatusMessage.setText(PreferenceManager.getString("statusMessage"));
        Glide.with(this).load(PreferenceManager.getString("profileImage")).into(myProfileImage);

        myProfileCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(view.getContext(), ProfileActivity.class);

                intent.putExtra("username", PreferenceManager.getString("username"));
                intent.putExtra("statusMessage", PreferenceManager.getString("statusMessage"));
                intent.putExtra("profileImage", PreferenceManager.getString("profileImage"));

                view.getContext().startActivity(intent);
            }
        });

        return view;
    }
}