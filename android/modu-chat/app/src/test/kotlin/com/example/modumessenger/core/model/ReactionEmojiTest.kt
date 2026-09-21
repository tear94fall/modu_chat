package com.example.modumessenger.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReactionEmojiTest {

    private val base = listOf(Reaction("LIKE", 2, listOf("a", "me")), Reaction("HEART", 1, listOf("b")))

    @Test
    fun `mine finds the emoji I left`() {
        assertEquals("LIKE", ReactionEmoji.mine(base, "me"))
        assertNull(ReactionEmoji.mine(base, "nobody"))
        assertNull(ReactionEmoji.mine(base, ""))
    }

    @Test
    fun `same emoji again removes mine and drops an emptied group`() {
        assertEquals(listOf(Reaction("LIKE", 1, listOf("a")), Reaction("HEART", 1, listOf("b"))), ReactionEmoji.toggle(base, "me", "LIKE"))
        assertEquals(listOf(Reaction("LIKE", 2, listOf("a", "me"))), ReactionEmoji.toggle(base, "b", "HEART"))
    }

    @Test
    fun `other emoji replaces mine, new emoji appends at the end`() {
        assertEquals(
            listOf(Reaction("LIKE", 1, listOf("a")), Reaction("HEART", 2, listOf("b", "me"))),
            ReactionEmoji.toggle(base, "me", "HEART"),
        )
        assertEquals(
            listOf(Reaction("LIKE", 1, listOf("a")), Reaction("HEART", 1, listOf("b")), Reaction("PRAY", 1, listOf("me"))),
            ReactionEmoji.toggle(base, "me", "PRAY"),
        )
        assertEquals(listOf(Reaction("WOW", 1, listOf("me"))), ReactionEmoji.toggle(emptyList(), "me", "WOW"))
    }
}
