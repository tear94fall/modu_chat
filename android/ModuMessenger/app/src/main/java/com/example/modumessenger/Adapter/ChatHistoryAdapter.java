package com.example.modumessenger.Adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.modumessenger.Activity.ProfileActivity;
import com.example.modumessenger.Activity.ProfileImageActivity;
import com.example.modumessenger.Global.PreferenceManager;
import com.example.modumessenger.R;
import com.example.modumessenger.Retrofit.RetrofitClient;
import com.example.modumessenger.dto.ChatType;
import com.example.modumessenger.entity.Member;
import com.example.modumessenger.RoomDatabase.Database.ChatDatabase;
import com.example.modumessenger.RoomDatabase.Entity.ChatEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChatHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public final int LEFT = 1;
    public final int RIGHT = 2;
    public final int LEFT_DUP = 3;
    public final int RIGHT_DUP = 4;
    public final int LEFT_IMAGE = 5;
    public final int RIGHT_IMAGE = 6;
    private final int LEFT_IMAGE_DUP = 5;
    private final int RIGHT_IMAGE_DUP = 6;

    private final List<Member> memberList;
    private final List<ChatBubble> chatList;
    private final String userId;

    ChatDatabase chatDatabase;

    public ChatHistoryAdapter(List<ChatBubble> chatList, List<Member> memberList) {
        this.memberList = (memberList == null || memberList.size() == 0) ? new ArrayList<>() : memberList;
        this.chatList = chatList;
        userId = PreferenceManager.getString("userId");
        sortChatBubble();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view;
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        chatDatabase = ChatDatabase.getInstance(parent.getContext());

        if (viewType == LEFT) {
            view = inflater.inflate(R.layout.chat_bubble_left, parent, false);
            return new ChatHistoryAdapter.ChatBubbleLeftViewHolder(view);
        } else if (viewType == RIGHT) {
            view = inflater.inflate(R.layout.chat_bubble_right, parent, false);
            return new ChatHistoryAdapter.ChatBubbleRightViewHolder(view);
        } else if (viewType == LEFT_DUP) {
            view = inflater.inflate(R.layout.chat_bubble_left_dup, parent, false);
            return new ChatHistoryAdapter.ChatBubbleLeftDuplicateViewHolder(view);
        } else if (viewType == RIGHT_DUP) {
            view = inflater.inflate(R.layout.chat_bubble_right_dup, parent, false);
            return new ChatHistoryAdapter.ChatBubbleRightDuplicateViewHolder(view);
        } else if (viewType == LEFT_IMAGE) {
            view = inflater.inflate(R.layout.chat_bubble_left_image, parent, false);
            return new ChatHistoryAdapter.ChatBubbleLeftImageViewHolder(view);
        } else if (viewType == RIGHT_IMAGE) {
            view = inflater.inflate(R.layout.chat_bubble_right_image, parent, false);
            return new ChatBubbleRightImageViewHolder(view);
        }  else {
            view = inflater.inflate(R.layout.chat_bubble_right, parent, false);
            return new ChatHistoryAdapter.ChatBubbleRightViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatBubble chat = chatList.get(position);
        ChatEntity chatEntity = new ChatEntity(chat);
        chatDatabase.chatDao().update(chatEntity);

        if (holder instanceof ChatBubbleLeftViewHolder) {
            ChatBubbleLeftViewHolder leftHolder = ((ChatBubbleLeftViewHolder) holder);

            Member member = memberList.stream()
                    .filter(m -> m.getUserId().equals(chat.getSender()))
                    .findFirst()
                    .orElse(null);

            if (member != null && member.getUserId().equals(chat.getSender())) {
                if (!member.getProfileImage().equals("") && member.getProfileImage() != null) {
                    Glide.with(((ChatBubbleLeftViewHolder) holder).senderImage)
                            .load(member.getProfileImage())
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                            .error(Glide.with(((ChatBubbleLeftViewHolder) holder).senderImage)
                                    .load(R.drawable.basic_profile_image)
                                    .into(((ChatBubbleLeftViewHolder) holder).senderImage))
                            .into(((ChatBubbleLeftViewHolder) holder).senderImage);
                } else {
                    Glide.with(((ChatBubbleLeftViewHolder) holder).senderImage)
                            .load(R.drawable.basic_profile_image)
                            .into(((ChatBubbleLeftViewHolder) holder).senderImage);
                }

                ((ChatBubbleLeftViewHolder) holder).setUserClickEvent(member);
            }

            leftHolder.chatSender.setText(chat.getSender());
            leftHolder.chatMessage.setText(chat.getChatMsg());
            leftHolder.leftChatTime.setText(getShortTime(chat.getChatTime()));
        } else if (holder instanceof ChatBubbleLeftImageViewHolder) {
            ChatBubbleLeftImageViewHolder leftImageViewHolder = ((ChatBubbleLeftImageViewHolder) holder);

            Member member = memberList.stream()
                    .filter(m -> m.getUserId().equals(chat.getSender()))
                    .findFirst()
                    .orElse(null);

            if(member != null && !member.getProfileImage().equals("") && member.getProfileImage()!=null) {
                Glide.with(((ChatBubbleLeftImageViewHolder) holder).senderImage)
                        .load(member.getProfileImage())
                        .diskCacheStrategy(DiskCacheStrategy.DATA)
                        .error(Glide.with(((ChatBubbleLeftImageViewHolder) holder).senderImage)
                                .load(R.drawable.basic_profile_image)
                                .into(((ChatBubbleLeftImageViewHolder) holder).senderImage))
                        .into(((ChatBubbleLeftImageViewHolder) holder).senderImage);
            } else {
                Glide.with(((ChatBubbleLeftImageViewHolder) holder).senderImage)
                        .load(R.drawable.basic_profile_image)
                        .into(((ChatBubbleLeftImageViewHolder) holder).senderImage);
            }

            ((ChatBubbleLeftImageViewHolder) holder).setUserClickEvent(member);
            ((ChatBubbleLeftImageViewHolder) holder).setChatImageClickEvent(chat);

            leftImageViewHolder.chatSender.setText(chat.getSender());
            Glide.with(leftImageViewHolder.chatImage)
                    .load(chat.getChatMsg())
                    .into(leftImageViewHolder.chatImage);
            leftImageViewHolder.leftChatTime.setText(getShortTime(chat.getChatTime()));
        } else if (holder instanceof ChatBubbleLeftDuplicateViewHolder) {
            ChatBubbleLeftDuplicateViewHolder leftDupHolder = ((ChatBubbleLeftDuplicateViewHolder) holder);
            leftDupHolder.chatMessage.setText(chat.getChatMsg());
            leftDupHolder.leftChatTime.setVisibility(View.INVISIBLE);
        } else if (holder instanceof ChatBubbleRightViewHolder) {
            ChatBubbleRightViewHolder rightHolder = ((ChatBubbleRightViewHolder) holder);
            rightHolder.chatMessage.setText(chat.getChatMsg());
            rightHolder.rightChatTime.setText(getShortTime(chat.getChatTime()));
        } else if (holder instanceof ChatBubbleRightImageViewHolder) {
            ChatBubbleRightImageViewHolder rightImageViewHolder = ((ChatBubbleRightImageViewHolder) holder);

            String accessToken = PreferenceManager.getString("access-token");
            String url = RetrofitClient.getBaseUrl() + "storage-service/view/"+ chat.getChatMsg();

            GlideUrl glideUrl = new GlideUrl(url,
                    new LazyHeaders.Builder()
                            .addHeader("Authorization", accessToken)
                            .build());

            Glide.with(rightImageViewHolder.chatImage)
                    .load(glideUrl)
                    .into(rightImageViewHolder.chatImage);

            ((ChatBubbleRightImageViewHolder) holder).setChatImageClickEvent(chat);

            rightImageViewHolder.rightChatTime.setText(getShortTime(chat.getChatTime()));
        } else if (holder instanceof ChatBubbleRightDuplicateViewHolder) {
            ChatBubbleRightDuplicateViewHolder rightDupHolder = ((ChatBubbleRightDuplicateViewHolder) holder);
            rightDupHolder.chatMessage.setText(chat.getChatMsg());
            rightDupHolder.leftChatTime.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return chatList.size();
    }

    @Override
    public int getItemViewType(int position) {
        ChatBubble chat = chatList.get(position);
        ChatBubble prevChat = chatList.get(Math.max(0, position - 1));

        if (!chat.getSender().equals(userId)) {
            if (position > 0 &&
                    chat.getSender().equals(prevChat.getSender()) &&
                    getShortTime(chat.getChatTime()).equals(getShortTime(prevChat.getChatTime()))) {

                return chat.getChatType() == ChatType.CHAT_TYPE_TEXT ? LEFT_DUP : LEFT_IMAGE_DUP;
            }

            return chat.getChatType() == ChatType.CHAT_TYPE_TEXT ? LEFT : LEFT_IMAGE;
        } else {
            if (position > 0 && getShortTime(chat.getChatTime()).equals(getShortTime(prevChat.getChatTime()))) {
                return chat.getChatType() == ChatType.CHAT_TYPE_TEXT ? RIGHT_DUP : RIGHT_IMAGE_DUP;
            }
        }

        return chat.getChatType() == ChatType.CHAT_TYPE_TEXT ? RIGHT : RIGHT_IMAGE;
    }

    public void sortChatBubble() {
        this.chatList.sort(Comparator.comparing(ChatBubble::getChatTime, Comparator.naturalOrder()));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addChatMsg(ChatBubble chatBubble) {
        this.chatList.add(chatBubble);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addChatMsgBack(List<ChatBubble> chatList) {
        this.chatList.addAll(chatList);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addChatMsgFront(List<ChatBubble> chatList) {
        chatList.forEach(chatBubble -> {
            this.chatList.add(0, chatBubble);
        });

        notifyDataSetChanged();
    }

    public String getShortTime(String time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime dateTime = LocalDateTime.parse(time, formatter);
        return dateTime.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
    }

    public void startProfileActivity(View view, Member member) {
        Intent intent = new Intent(view.getContext(), ProfileActivity.class);
        intent.putExtra("email", member.getEmail());
        intent.putExtra("userId", member.getUserId());
        intent.putExtra("username", member.getUsername());
        intent.putExtra("statusMessage", member.getStatusMessage());
        intent.putExtra("profileImage", member.getProfileImage());
        intent.putExtra("wallpaperImage", member.getWallpaperImage());

        view.getContext().startActivity(intent);
    }

    public class ChatBubbleLeftViewHolder extends RecyclerView.ViewHolder {
        ImageView senderImage;
        TextView chatMessage;
        TextView leftChatTime;
        TextView chatSender;

        public ChatBubbleLeftViewHolder(@NonNull View itemView) {
            super(itemView);
            senderImage = itemView.findViewById(R.id.message_sender_image);
            chatMessage = itemView.findViewById(R.id.left_chat_text);
            chatSender = itemView.findViewById(R.id.message_sender);
            leftChatTime = itemView.findViewById(R.id.left_chat_time);
        }

        public void setUserClickEvent(Member member) {
            this.senderImage.setOnClickListener(v -> {
                startProfileActivity(v, member);
            });
        }
    }

    public class ChatBubbleLeftImageViewHolder extends RecyclerView.ViewHolder {
        ImageView senderImage;
        ImageView chatImage;
        TextView leftChatTime;
        TextView chatSender;

        public ChatBubbleLeftImageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderImage = itemView.findViewById(R.id.message_sender_image);
            chatImage = itemView.findViewById(R.id.left_chat_image);
            chatSender = itemView.findViewById(R.id.message_sender);
            leftChatTime = itemView.findViewById(R.id.left_chat_time);
        }

        public void setUserClickEvent(Member member) {
            this.senderImage.setOnClickListener(v -> {
                startProfileActivity(v, member);
            });
        }

        public void setChatImageClickEvent(ChatBubble chat) {
            this.chatImage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileImageActivity.class);

                ArrayList<String> imageUrlList = new ArrayList<>();
                imageUrlList.add(chat.getChatMsg());
                intent.putStringArrayListExtra("imageUrlList", imageUrlList);

                v.getContext().startActivity(intent);
            });
        }
    }

    public static class ChatBubbleLeftDuplicateViewHolder extends RecyclerView.ViewHolder {
        TextView chatMessage;
        TextView leftChatTime;

        public ChatBubbleLeftDuplicateViewHolder(@NonNull View itemView) {
            super(itemView);
            chatMessage = itemView.findViewById(R.id.left_chat_text);
            leftChatTime = itemView.findViewById(R.id.left_chat_time);
        }
    }

    public static class ChatBubbleRightViewHolder extends RecyclerView.ViewHolder {
        TextView chatMessage;
        TextView rightChatTime;

        public ChatBubbleRightViewHolder(@NonNull View itemView) {
            super(itemView);
            chatMessage = itemView.findViewById(R.id.right_chat_text);
            rightChatTime = itemView.findViewById(R.id.right_chat_time);
        }
    }

    public static class ChatBubbleRightImageViewHolder extends RecyclerView.ViewHolder {
        ImageView chatImage;
        TextView rightChatTime;

        public ChatBubbleRightImageViewHolder(@NonNull View itemView) {
            super(itemView);
            chatImage = itemView.findViewById(R.id.right_chat_image);
            rightChatTime = itemView.findViewById(R.id.right_chat_time);
        }

        public void setChatImageClickEvent(ChatBubble chat) {
            this.chatImage.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), ProfileImageActivity.class);

                ArrayList<String> imageUrlList = new ArrayList<>();
                imageUrlList.add(chat.getChatMsg());
                intent.putStringArrayListExtra("imageUrlList", imageUrlList);

                v.getContext().startActivity(intent);
            });
        }
    }

    public static class ChatBubbleRightDuplicateViewHolder extends RecyclerView.ViewHolder {
        TextView chatMessage;
        TextView leftChatTime;

        public ChatBubbleRightDuplicateViewHolder(@NonNull View itemView) {
            super(itemView);
            chatMessage = itemView.findViewById(R.id.right_chat_text);
            leftChatTime = itemView.findViewById(R.id.right_chat_time);
        }
    }
}
