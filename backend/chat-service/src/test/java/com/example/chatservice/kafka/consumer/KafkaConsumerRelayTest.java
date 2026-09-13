package com.example.chatservice.kafka.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.example.chatservice.kafka.producer.KafkaProducerService;
import com.example.chatservice.message.entity.ChatMessage;
import com.example.chatservice.message.entity.SubscribeType;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * ws-service 가 1:1 차단 때 실어 보내는 excludeUserIds 는 chat-service 를 그냥 지나가야 한다.
 * 여기서 값이 떨어지면 다른 ws 인스턴스가 제외 대상을 몰라 차단이 새 나간다.
 *
 * 실제 컨슈머가 쓰는 JsonDeserializer(KafkaConsumerConfig 와 같은 설정)로 역직렬화해
 * 릴레이까지 태운다.
 */
class KafkaConsumerRelayTest {

    private static final String TOPIC = "topic-chat-save";

    private ChatMessage deserialize(String json) {
        try (JsonDeserializer<ChatMessage> deserializer = new JsonDeserializer<>(ChatMessage.class, false)) {
            deserializer.addTrustedPackages("*");
            return deserializer.deserialize(TOPIC, json.getBytes(StandardCharsets.UTF_8));
        }
    }

    private ChatMessage relay(ChatMessage incoming) {
        KafkaProducerService producer = mock(KafkaProducerService.class);
        KafkaConsumerService consumer = new KafkaConsumerService(producer);

        consumer.receive(new ConsumerRecord<>(TOPIC, 0, 0L, "key", incoming), mock(Acknowledgment.class));

        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(producer).sendMessage(org.mockito.ArgumentMatchers.eq(incoming.getRoomId()), captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("excludeUserIds 가 실린 메시지는 같은 값 그대로 브로드캐스트로 넘어간다")
    void relayKeepsExcludeUserIds() {
        ChatMessage incoming = deserialize(
                "{\"type\":\"BROAD_CAST\",\"roomId\":\"r1\",\"chatId\":\"10\",\"excludeUserIds\":[\"blocked-user\"]}");

        assertThat(incoming.getExcludeUserIds()).containsExactly("blocked-user");

        ChatMessage relayed = relay(incoming);

        assertThat(relayed).isSameAs(incoming);
        assertThat(relayed.getExcludeUserIds()).containsExactly("blocked-user");
    }

    @Test
    @DisplayName("필드가 없던 옛 메시지도 그대로 역직렬화되고 릴레이된다")
    void legacyMessageWithoutFieldStillWorks() {
        ChatMessage incoming = deserialize("{\"type\":\"BROAD_CAST\",\"roomId\":\"r1\",\"chatId\":\"10\"}");

        assertThat(incoming.getExcludeUserIds()).isNull();

        ChatMessage relayed = relay(incoming);

        assertThat(relayed.getRoomId()).isEqualTo("r1");
        assertThat(relayed.getChatId()).isEqualTo("10");
        assertThat(relayed.getExcludeUserIds()).isNull();
    }

    @Test
    @DisplayName("모르는 필드가 섞여 와도 깨지지 않는다")
    void unknownFieldsAreIgnored() {
        ChatMessage incoming = deserialize(
                "{\"type\":\"BROAD_CAST\",\"roomId\":\"r1\",\"chatId\":\"10\",\"somethingNew\":true}");

        assertThat(incoming.getRoomId()).isEqualTo("r1");
    }

    @Test
    @DisplayName("제외 대상이 없으면 JSON 에 키를 넣지 않아 기존 메시지 모양 그대로다")
    void nullExcludeUserIdsIsOmittedFromJson() {
        try (JsonSerializer<ChatMessage> serializer = new JsonSerializer<>()) {
            String json = new String(
                    serializer.serialize(TOPIC, new ChatMessage(SubscribeType.BROAD_CAST, "r1", "10")),
                    StandardCharsets.UTF_8);

            assertThat(json).doesNotContain("excludeUserIds");
        }
    }

    @Test
    @DisplayName("제외 대상이 있으면 직렬화 결과에 남는다")
    void excludeUserIdsSurvivesSerialization() {
        try (JsonSerializer<ChatMessage> serializer = new JsonSerializer<>()) {
            ChatMessage message = new ChatMessage(SubscribeType.BROAD_CAST, "r1", "10", List.of("blocked-user"));
            String json = new String(serializer.serialize(TOPIC, message), StandardCharsets.UTF_8);

            assertThat(json).contains("\"excludeUserIds\":[\"blocked-user\"]");
            assertThat(deserialize(json).getExcludeUserIds()).containsExactly("blocked-user");
        }
    }
}
