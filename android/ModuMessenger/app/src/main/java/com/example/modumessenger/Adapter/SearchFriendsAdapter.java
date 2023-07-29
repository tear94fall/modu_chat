package com.example.modumessenger.Adapter;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;
import static com.example.modumessenger.Global.GlideUtil.setProfileImage;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.Activity.SearchActivity;
import com.example.modumessenger.R;
import com.example.modumessenger.dto.MemberDto;
import com.example.modumessenger.entity.Member;

import java.util.List;

public class SearchFriendsAdapter extends RecyclerView.Adapter<SearchFriendsAdapter.SearchFriendsViewHolder> {

    List<MemberDto> searchMemberList;
    Member member;

    public SearchFriendsAdapter(List<MemberDto> searchMemberList) {
        this.searchMemberList = searchMemberList;
        member = getDataStoreMember();
    }

    @NonNull
    @Override
    public SearchFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.friend_search_row, parent, false);
        return new SearchFriendsViewHolder(parent.getContext(), view);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchFriendsViewHolder holder, int position) {
        MemberDto memberDto = this.searchMemberList.get(position);

        holder.setUserInfo(memberDto);
        holder.setUserClickEvent(memberDto);
        holder.setAddFriendsButton(memberDto);

        searchMemberList.forEach(m -> {
            // exclude my info
            if(member.getUserId().equals(memberDto.getUserId())) {
                holder.addFriendsButton.setVisibility(View.INVISIBLE);
            }
            // need to add exclude already friends info
        });
    }

    @Override
    public int getItemCount() {
        return searchMemberList.size();
    }

    public static class SearchFriendsViewHolder extends RecyclerView.ViewHolder {
        Context context;

        TextView username;
        TextView statusMessage;
        ImageView profileImage;
        Button addFriendsButton;
        ConstraintLayout cardViewLayout;

        public SearchFriendsViewHolder(Context context, @NonNull View itemView) {
            super(itemView);

            this.context = context;
            username = itemView.findViewById(R.id.search_user_name);
            statusMessage = itemView.findViewById(R.id.search_status_message);
            profileImage = itemView.findViewById(R.id.search_profile_image);
            addFriendsButton = itemView.findViewById(R.id.add_friends_button);
            cardViewLayout = itemView.findViewById(R.id.searchFriendCardViewLayout);
        }

        public void setUserInfo(MemberDto member) {
            this.username.setText(member.getUsername());
            this.statusMessage.setText(member.getStatusMessage());
            setProfileImage(profileImage, member.getProfileImage());
        }

        public void setUserClickEvent(MemberDto member) {
            this.cardViewLayout.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileActivity.class);
                intent.putExtra("memberId", String.valueOf(member.getId()));

                v.getContext().startActivity(intent);
            });
        }

        public void setAddFriendsButton(MemberDto member) {
            this.addFriendsButton.setOnClickListener(view -> {
                ((SearchActivity) this.context).addFriendByEmail(member);
                this.addFriendsButton.setVisibility(View.INVISIBLE);
            });
        }
    }
}
