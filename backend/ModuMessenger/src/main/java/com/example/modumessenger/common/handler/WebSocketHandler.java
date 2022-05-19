package com.example.modumessenger.common.handler;

import com.example.modumessenger.chat.entity.Chat;
import com.example.modumessenger.chat.entity.ChatRoom;
import com.example.modumessenger.chat.repository.ChatRepository;
import com.example.modumessenger.chat.repository.ChatRoomRepository;
import com.example.modumessenger.common.parser.JsonParser;
import com.example.modumessenger.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Transactional
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;

    private static final ConcurrentHashMap<String, WebSocketSession> CLIENTS = new ConcurrentHashMap<String, WebSocketSession>();
    private static final String FILE_UPLOAD_PATH = "/modu-chat/images";

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws ParseException, IOException {
        String payload = message.getPayload();
        System.out.println(payload);

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new StringReader(payload));

        String roomId = (String) jsonObject.get("roomId");
        String msg = (String) jsonObject.get("message");
        String sender = Objects.requireNonNull(session.getHandshakeHeaders().get("userId")).get(0);

        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId);

        Chat chat = new Chat(msg, roomId, chatRoom, sender);
        chatRepository.save(chat);

        for (String userId : chatRoom.getUserIds()) {
            if(!sender.equals(userId)) {
                WebSocketSession s = CLIENTS.get(userId);
                if (s != null) {
                    try {
                        s.sendMessage(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String url = Objects.requireNonNull(session.getUri()).toString();
        String roomId = url.split("/modu-chat/")[1];
        String sender = Objects.requireNonNull(session.getHandshakeHeaders().get("userId")).get(0);

        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId);

        ByteBuffer byteBuffer = message.getPayload();
        boolean save = saveImageFile(byteBuffer);
        byteBuffer.position(0);

        if(save) {
            for (String userId : chatRoom.getUserIds()) {
                if (!sender.equals(userId)) {
                    WebSocketSession s = CLIENTS.get(userId);
                    if (s != null) {
                        try {
                            s.sendMessage(new BinaryMessage(byteBuffer));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        String url = Objects.requireNonNull(session.getUri()).toString();
        String roomId = url.split("/modu-chat/")[1];
        String userId = Objects.requireNonNull(session.getHandshakeHeaders().get("userId")).get(0);

        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId);
        if(chatRoom!=null) {
            CLIENTS.put("userId", session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);

        String url = Objects.requireNonNull(session.getUri()).toString();
        String roomId = url.split("/modu-chat/")[1];
        String userId = Objects.requireNonNull(session.getHandshakeHeaders().get("userId")).get(0);

        if(CLIENTS.get(userId)!=null) {
            CLIENTS.remove(userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

    }

    public boolean saveImageFile(ByteBuffer byteBuffer) {
        String fileName = "temp.jpg";
        File dir = new File(FILE_UPLOAD_PATH);

        if(!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(FILE_UPLOAD_PATH, fileName);
        FileOutputStream out = null;
        FileChannel outChannel = null;

        try {
            byteBuffer.flip();
            out = new FileOutputStream(file, true);
            outChannel = out.getChannel();
            byteBuffer.compact();
            outChannel.write(byteBuffer);
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if(out != null) {
                    out.close();
                }
                if(outChannel != null) {
                    outChannel.close();
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }

        byteBuffer.position(0);

        return true;
    }
}
