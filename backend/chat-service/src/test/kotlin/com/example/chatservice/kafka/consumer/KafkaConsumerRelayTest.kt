package com.example.chatservice.kafka.consumer

import com.example.chatservice.kafka.producer.KafkaProducerService
import com.example.chatservice.message.entity.ChatMessage
import com.example.chatservice.message.entity.SubscribeType
import java.nio.charset.StandardCharsets
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

/**
 * ws-service 가 1:1 차단 때 실어 보내는 excludeUserIds 는 chat-service 를 그냥 지나가야 한다.
 * 여기서 값이 떨어지면 다른 ws 인스턴스가 제외 대상을 몰라 차단이 새 나간다.
 *
 * 실제 컨슈머가 쓰는 JsonDeserializer(KafkaConsumerConfig 와 같은 설정)로 역직렬화해
 * 릴레이까지 태운다.
 */
class KafkaConsumerRelayTest {

    companion object {
        private const val TOPIC = "topic-chat-save"
    }

    private fun deserialize(json: String): ChatMessage =
        JsonDeserializer(ChatMessage::class.java, false).use { deserializer ->
            deserializer.addTrustedPackages("*")
            deserializer.deserialize(TOPIC, json.toByteArray(StandardCharsets.UTF_8))!!
        }

    private fun relay(incoming: ChatMessage): ChatMessage {
        val producer = mock<KafkaProducerService>()
        val consumer = KafkaConsumerService(producer)

        consumer.receive(ConsumerRecord(TOPIC, 0, 0L, "key", incoming), mock<Acknowledgment>())

        val captor = argumentCaptor<ChatMessage>()
        verify(producer).sendMessage(eq(incoming.roomId!!), captor.capture())
        return captor.firstValue
    }

    @Test
    @DisplayName("excludeUserIds 가 실린 메시지는 같은 값 그대로 브로드캐스트로 넘어간다")
    fun relayKeepsExcludeUserIds() {
        val incoming = deserialize(
            "{\"type\":\"BROAD_CAST\",\"roomId\":\"r1\",\"chatId\":\"10\",\"excludeUserIds\":[\"blocked-user\"]}",
        )

        assertThat(incoming.excludeUserIds).containsExactly("blocked-user")

        val relayed = relay(incoming)

        assertThat(relayed).isSameAs(incoming)
        assertThat(relayed.excludeUserIds).containsExactly("blocked-user")
    }

    @Test
    @DisplayName("필드가 없던 옛 메시지도 그대로 역직렬화되고 릴레이된다")
    fun legacyMessageWithoutFieldStillWorks() {
        val incoming = deserialize("{\"type\":\"BROAD_CAST\",\"roomId\":\"r1\",\"chatId\":\"10\"}")

        assertThat(incoming.excludeUserIds).isNull()

        val relayed = relay(incoming)

        assertThat(relayed.roomId).isEqualTo("r1")
        assertThat(relayed.chatId).isEqualTo("10")
        assertThat(relayed.excludeUserIds).isNull()
    }

    @Test
    @DisplayName("모르는 필드가 섞여 와도 깨지지 않는다")
    fun unknownFieldsAreIgnored() {
        val incoming = deserialize("{\"type\":\"BROAD_CAST\",\"roomId\":\"r1\",\"chatId\":\"10\",\"somethingNew\":true}")

        assertThat(incoming.roomId).isEqualTo("r1")
    }

    @Test
    @DisplayName("제외 대상이 없으면 JSON 에 키를 넣지 않아 기존 메시지 모양 그대로다")
    fun nullExcludeUserIdsIsOmittedFromJson() {
        JsonSerializer<ChatMessage>().use { serializer ->
            val json = String(serializer.serialize(TOPIC, ChatMessage(SubscribeType.BROAD_CAST, "r1", "10"))!!, StandardCharsets.UTF_8)

            assertThat(json).doesNotContain("excludeUserIds")
        }
    }

    @Test
    @DisplayName("제외 대상이 있으면 직렬화 결과에 남는다")
    fun excludeUserIdsSurvivesSerialization() {
        JsonSerializer<ChatMessage>().use { serializer ->
            val message = ChatMessage(SubscribeType.BROAD_CAST, "r1", "10", listOf("blocked-user"))
            val json = String(serializer.serialize(TOPIC, message)!!, StandardCharsets.UTF_8)

            assertThat(json).contains("\"excludeUserIds\":[\"blocked-user\"]")
            assertThat(deserialize(json).excludeUserIds).containsExactly("blocked-user")
        }
    }
}
