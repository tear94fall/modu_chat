package com.example.modumessenger.common.handler;

import com.example.modumessenger.chat.dto.ChatDto;
import com.example.modumessenger.chat.dto.ChatRoomDto;
import com.example.modumessenger.chat.service.ChatRoomService;
import com.example.modumessenger.chat.service.ChatService;
import com.example.modumessenger.fcm.service.FcmService;
import com.example.modumessenger.member.dto.MemberDto;
import com.example.modumessenger.member.service.MemberService;
import com.example.modumessenger.messaging.entity.ChatMessage;
import com.example.modumessenger.messaging.entity.SubscribeType;
import com.example.modumessenger.messaging.service.MessagingPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.firebase.messaging.FirebaseMessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler implements MessageListener {

    private final MemberService memberService;
    private final ChatRoomService chatRoomService;
    private final ChatService chatService;
    private final FcmService fcmService;
    private final MessagingPublisher messagingPublisher;

    private final ObjectMapper objectMapper;

    private static final ConcurrentHashMap<String, WebSocketSession> CLIENTS = new ConcurrentHashMap<String, WebSocketSession>();
    private static final String FILE_UPLOAD_PATH = "/modu-chat/images";
    private static final String CHAT_MESSAGING_TOPIC_NAME = "modu-chat";

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws ParseException, IOException, FirebaseMessagingException {
        String payload = message.getPayload();
        System.out.println(payload);

        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(new StringReader(payload));

        String senderId = Objects.requireNonNull(session.getHandshakeHeaders().get("userId")).get(0);

        MemberDto sender = memberService.getUserById(senderId);

        String roomId = (String) jsonObject.get("roomId");
        String msg = (String) jsonObject.get("message");
        String senderName = sender.getUserId();
        String sendTime = (String) jsonObject.get("chatTime");
        Long chatType = (Long) jsonObject.get("chatType");

        ChatRoomDto chatRoomDto = chatRoomService.searchChatRoomByRoomId(roomId);

        List<MemberDto> members = chatRoomDto.getMembers().stream().filter(memberDto -> memberDto.getUserId().equals(senderName)).collect(Collectors.toList());
        if(members.size()==0) return;

        chatRoomDto.setLastChatTime(sendTime);

        if(chatType == ChatType.TEXT.getChatType()) {
            chatRoomDto.setLastChatMsg(msg);
        } else {
            chatRoomDto.setLastChatMsg(ChatType.fromChatType(chatType.intValue()).getChatTypeStr());
        }

        ChatDto chatDto = new ChatDto(msg, roomId, senderName, sendTime, chatType.intValue(), chatRoomDto);
        Long chatId = chatService.saveChat(chatDto);

        chatRoomDto.setLastChatId(chatId.toString());
        chatRoomService.updateChatRoom(chatRoomDto.getRoomId(), chatRoomDto);

        ChatMessage chatMessage = new ChatMessage(SubscribeType.BROAD_CAST, chatRoomDto.getRoomId(), chatId.toString());

        ChannelTopic channel = new ChannelTopic(CHAT_MESSAGING_TOPIC_NAME);
        messagingPublisher.publish(channel, chatMessage);

        Map<String, String> data = new HashMap<>() {
            {
                put("roomId", chatRoomDto.getRoomId());
                put("sender", chatDto.getSender());
            }
        };

        fcmService.sendTopicMessageWithData(chatRoomDto.getRoomId(), chatRoomDto.getRoomName(), chatDto.getMessage(), null, data);
    }

    @Override
    public void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        String url = Objects.requireNonNull(session.getUri()).toString();
        String roomId = url.split("/modu-chat/")[1];
        String sender = Objects.requireNonNull(session.getHandshakeHeaders().get("userId")).get(0);

        ChatRoomDto chatRoomDto = chatRoomService.searchChatRoomByRoomId(roomId);

        ByteBuffer byteBuffer = message.getPayload();
        boolean save = saveImageFile(byteBuffer);
        byteBuffer.position(0);

        if(save) {
            chatRoomDto.getMembers().forEach(memberDto -> {
                String userId = memberDto.getUserId();
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
            });
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);

        String url = Objects.requireNonNull(session.getUri()).toString();
        String roomId = url.split("/modu-chat/")[1];
        String userId = Objects.requireNonNull(session.getHandshakeHeaders().get("userId")).get(0);

        ChatRoomDto chatRoomDto = chatRoomService.searchChatRoomByRoomId(roomId);

        if(chatRoomDto!=null) {
            CLIENTS.put(userId, session);
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

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String subscribeMessage = new String(message.getBody());
            ChatMessage chatMessage = objectMapper.readValue(subscribeMessage, ChatMessage.class);
            log.info("[subscribe][message] {}", chatMessage);

            ChatRoomDto chatRoomDto = chatRoomService.searchChatRoomByRoomId(chatMessage.getRoomId());
            ChatDto chatDto = chatService.searchChatByRoomIdAndChatId(chatMessage.getRoomId(), chatMessage.getChatId());
            String payload = objectMapper.writeValueAsString(chatDto);

            TextMessage textMessage = new TextMessage(payload);

            chatRoomDto.getMembers().forEach(member -> {
                String userId = member.getUserId();
                WebSocketSession s = CLIENTS.get(userId);
                if (s != null) {
                    try {
                        s.sendMessage(textMessage);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    enum ChatType {
        TEXT(1, ""),
        IMAGE(2, "image"),
        FILE(3, "file"),
        AUDIO(4, "audio");

        private final int chatType;
        private final String chatTypeStr;

        private static final Map<Integer, ChatType> chatTypeMap = new HashMap<>();

        static {
            for(ChatType chatType : values()) {
                chatTypeMap.put(chatType.getChatType(), chatType);
            }
        }

        ChatType(int chatType, String chatTypeStr) {
            this.chatType = chatType;
            this.chatTypeStr = chatTypeStr;
        }

        public int getChatType() { return this.chatType; }
        public String getChatTypeStr() { return this.chatTypeStr; }


        public static ChatType fromChatType(int type) {
            return chatTypeMap.get(type);
        }
    }
}
