package com.example.modumessenger.Global;

import android.annotation.SuppressLint;
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
import com.example.modumessenger.Global.socket.ConnectionState;
import com.example.modumessenger.R;
import com.example.modumessenger.dto.ChatType;
import com.example.modumessenger.dto.FcmMessageDto;
import com.example.modumessenger.entity.Member;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FirebaseChatMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("Firebase", "FirebaseChatMessagingService : " + s);
    }

    /**
     * 매 메시지마다 다시 읽는다. 생성자에서 읽으면 두 가지가 깨진다.
     * 하나, 시스템은 로그인 전/로그아웃 후에도 이 서비스를 만드는데
     * getDataStoreMember() 는 저장된 member 가 없으면 NPE 를 던져 서비스 생성이 실패하고
     * FCM 알림이 통째로 사라진다. 둘, 프로세스 수명 동안 계정이 바뀌면
     * 생성 시점에 캡처한 신원으로 판정하게 된다.
     */
    private String currentUserId() {
        if (!Boolean.TRUE.equals(DataStoreHelper.checkDataStoreKey("member"))) return null;

        Member member = DataStoreHelper.getDataStoreMember();
        return member == null ? null : member.getUserId();
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        if (remoteMessage.getData().size() == 0) return;

        ObjectMapper mapper = new ObjectMapper();
        FcmMessageDto fcmMessageDto = mapper.convertValue(remoteMessage.getData(), FcmMessageDto.class);

        String myUserId = currentUserId();
        if (myUserId != null && myUserId.equals(fcmMessageDto.getSender())) return;

        // 앱이 보이고 소켓이 살아 있으면 인앱 배너가 알림을 담당한다.
        // 재연결 중에는 배너가 뜨지 않으므로 FCM 이 알려야 한다.
        boolean socketHandlesIt = AppLifecycleObserver.isForeground()
                && App.getWebSocketManager() != null
                && App.getWebSocketManager().getState() == ConnectionState.CONNECTED;

        if (socketHandlesIt) return;

        setPushAlarm(remoteMessage);
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
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

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
                            .setSmallIcon(R.drawable.ic_notification)
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
                            .setSmallIcon(R.drawable.ic_notification)
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
