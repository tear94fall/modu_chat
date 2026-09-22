package com.example.modumessenger.core.model

/** 메시지 하나의 이모지별 반응 집계. [userIds] 로 내가 남겼는지·누가 남겼는지 안다. */
data class Reaction(
    val emoji: String,
    val count: Int,
    val userIds: List<String>,
)

/** 서버가 저장하는 이모지 키와 화면에 그릴 글자. 순서가 곧 선택 바의 순서다. */
object ReactionEmoji {
    val ALL: List<String> = listOf("LIKE", "HEART", "LAUGH", "WOW", "SAD", "PRAY")

    private val TEXT = mapOf(
        "LIKE" to "👍", "HEART" to "❤️", "LAUGH" to "😂", "WOW" to "😮", "SAD" to "😢", "PRAY" to "🙏",
    )

    fun text(key: String): String = TEXT[key] ?: key

    /** [userId] 가 이 메시지에 남긴 이모지 키. 없으면 null. */
    fun mine(reactions: List<Reaction>, userId: String): String? =
        reactions.firstOrNull { userId.isNotEmpty() && userId in it.userIds }?.emoji

    /**
     * 서버 확정 전에 화면에 먼저 반영할 때. 같은 이모지면 내 것을 빼고, 아니면 다른 이모지에서 빼고 이 이모지에 넣는다.
     * 서버(ReactionSummaryDto.summarize)와 같은 규칙: 처음 남겨진 이모지가 앞, 0 이 된 이모지는 사라진다.
     */
    fun toggle(reactions: List<Reaction>, userId: String, emoji: String): List<Reaction> {
        val current = mine(reactions, userId)
        val removed = reactions.mapNotNull { r ->
            if (userId !in r.userIds) r
            else {
                val rest = r.userIds - userId
                if (rest.isEmpty()) null else r.copy(count = rest.size, userIds = rest)
            }
        }
        if (current == emoji) return removed
        val existing = removed.firstOrNull { it.emoji == emoji }
        return if (existing == null) {
            removed + Reaction(emoji, 1, listOf(userId))
        } else {
            removed.map { if (it.emoji == emoji) it.copy(count = it.count + 1, userIds = it.userIds + userId) else it }
        }
    }
}
