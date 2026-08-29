package com.example.modumessenger.Global;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.example.modumessenger.Activity.ChatActivity;
import com.example.modumessenger.Repository.BannerEvent;
import com.example.modumessenger.dto.ChatType;
import com.google.android.material.snackbar.Snackbar;

public final class ChatBanner {

    private ChatBanner() {
    }

    public static void show(Activity activity, BannerEvent event) {
        if (activity == null || event == null) return;

        View root = activity.findViewById(android.R.id.content);
        if (root == null) return;

        String text = event.getSender() + " : " + preview(event);

        Snackbar.make(root, text, Snackbar.LENGTH_LONG)
                .setAction("이동", v -> {
                    Intent intent = new Intent(activity, ChatActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("roomId", event.getRoomId());
                    activity.startActivity(intent);
                })
                .show();
    }

    private static String preview(BannerEvent event) {
        switch (event.getChatType()) {
            case ChatType.CHAT_TYPE_IMAGE:
                return "사진";
            case ChatType.CHAT_TYPE_FILE:
                return "파일";
            case ChatType.CHAT_TYPE_AUDIO:
                return "음성";
            default:
                return event.getMessage();
        }
    }
}
