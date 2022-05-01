package com.example.modumessenger.Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Adapter.FriendsAdapter;
import com.example.modumessenger.R;

public class FragmentFriends extends Fragment {

    RecyclerView recyclerView;
    RecyclerView.LayoutManager layoutManager;

    String s1[], s2[];
    int images[] = {
            R.drawable.blank_profile_image, R.drawable.blank_profile_image,
            R.drawable.blank_profile_image, R.drawable.blank_profile_image,
            R.drawable.blank_profile_image, R.drawable.blank_profile_image,
            R.drawable.blank_profile_image, R.drawable.blank_profile_image,
            R.drawable.blank_profile_image, R.drawable.blank_profile_image,
            R.drawable.blank_profile_image, R.drawable.blank_profile_image,
            R.drawable.blank_profile_image, R.drawable.blank_profile_image
    };

    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.friend_recycler_view);
        recyclerView.setHasFixedSize(true);

        layoutManager = new LinearLayoutManager(getActivity());

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.scrollToPosition(0);

        s1 = getResources().getStringArray(R.array.friend_name);
        s2 = getResources().getStringArray(R.array.description);

        FriendsAdapter myAdapter = new FriendsAdapter(s1, s2, images);
        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(layoutManager);

        return view;
    }
}