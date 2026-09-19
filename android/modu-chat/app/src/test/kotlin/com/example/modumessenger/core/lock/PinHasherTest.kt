package com.example.modumessenger.core.lock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    private val hasher = PinHasher(iterations = 100)

    @Test
    fun `같은 PIN 과 salt 는 같은 해시를 낸다`() {
        val salt = hasher.newSalt()
        assertEquals(hasher.hash("1234", salt), hasher.hash("1234", salt))
    }

    @Test
    fun `salt 가 다르면 해시가 다르다`() {
        assertNotEquals(hasher.hash("1234", hasher.newSalt()), hasher.hash("1234", hasher.newSalt()))
    }

    @Test
    fun `matches 는 맞는 PIN 만 통과시킨다`() {
        val salt = hasher.newSalt()
        val hash = hasher.hash("1234", salt)
        assertTrue(hasher.matches("1234", salt, hash))
        assertFalse(hasher.matches("1235", salt, hash))
        assertFalse(hasher.matches("", salt, hash))
    }
}
