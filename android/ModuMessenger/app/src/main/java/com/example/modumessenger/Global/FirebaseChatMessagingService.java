package com.example.modumessenger.Global;

import static com.example.modumessenger.Global.DataStoreHelper.getDataStoreMember;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.modumessenger.Activity.ChatActivity;
import com.example.modumessenger.R;
import com.example.modumessenger.dto.ChatType;
import com.example.modumessenger.dto.FcmMessageDto;
import com.example.modumessenger.entity.Member;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Objects;

public class FirebaseChatMessagingService extends FirebaseMessagingService {

    private final Member member;

    public FirebaseChatMessagingService() {
        member = getDataStoreMember();
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("Firebase", "FirebaseChatMessagingService : " + s);
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() > 0) {
            ObjectMapper mapper = new ObjectMapper();
            FcmMessageDto fcmMessageDto = mapper.convertValue(remoteMessage.getData(), FcmMessageDto.class);

            if(!Objects.requireNonNull(fcmMessageDto.getSender()).equals(member.getUserId())) {
                ActivityStack activityStack =  ActivityStack.getInstance();
                Activity foregroundActivity = activityStack.getForegroundActivity();

                if(foregroundActivity instanceof ChatActivity) {
                    ChatActivity chatActivity = (ChatActivity) foregroundActivity;

                    if(!chatActivity.getRoomId().equals(fcmMessageDto.getRoomId())) {
                        setPushAlarm(remoteMessage);
                    }
                } else {
                    setPushAlarm(remoteMessage);
                }
            }
        }
    }

    private void setPushAlarm(RemoteMessage remoteMessage) {
        sendNotification(remoteMessage);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(getApplicationContext(), "새로운 메시지가 도착하였습니다.", Toast.LENGTH_LONG).show());
    }

    @SuppressLint("ObsoleteSdkInt")
    private void sendNotification(RemoteMessage remoteMessage) {

        String type = remoteMessage.getData().get("type");
        String title = remoteMessage.getData().get("title");
        String message = remoteMessage.getData().get("message");

        int chatType = Integer.parseInt(type);

        if (chatType == ChatType.CHAT_TYPE_IMAGE) {
            message = "새로운 사진";
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("roomId", remoteMessage.getData().get("roomId"));
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channel = "modu-chat";
            String channel_nm = "modu-chat-channel";

            NotificationManager notificationChannel = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channelMessage = new NotificationChannel(channel, channel_nm, android.app.NotificationManager.IMPORTANCE_DEFAULT);
            channelMessage.setDescription("modu-chat-messaging-channel");
            channelMessage.enableLights(true);
            channelMessage.enableVibration(true);
            channelMessage.setShowBadge(false);
            channelMessage.setVibrationPattern(new long[]{100, 200, 100, 200});
            notificationChannel.createNotificationChannel(channelMessage);

            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, channel)
                            .setSmallIcon(R.drawable.modu_icon)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setChannelId(channel)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(9999, notificationBuilder.build());
        } else {
            NotificationCompat.Builder notificationBuilder =
                    new NotificationCompat.Builder(this, "")
                            .setSmallIcon(R.drawable.modu_icon)
                            .setContentTitle(title)
                            .setContentText(message)
                            .setAutoCancel(true)
                            .setContentIntent(pendingIntent)
                            .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(9999, notificationBuilder.build());
        }
    }
}
