package com.example.pointservice.api.member

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

/** member-service 어댑터: 응답을 포트 모양으로 바꾸고, 실패는 빈 결과로 삼킨다. */
class FeignMemberLookupTest {

    private val client: MemberFeignClient = mock()
    private val lookup = FeignMemberLookup(client)

    @Test
    fun byUserIds_mapsByUserId_andSkipsRowsWithoutId() {
        whenever(client.getMembersByUserId(eq(listOf("a", "b")))).thenReturn(
            listOf(MemberSummaryDto("a", "앨리스", "alice@example.com"), MemberSummaryDto(null, "이름만", null)),
        )

        val found = lookup.byUserIds(listOf("a", "b", "a"))

        assertThat(found.keys).containsExactly("a")
        assertThat(found.getValue("a").username).isEqualTo("앨리스")
        assertThat(found.getValue("a").email).isEqualTo("alice@example.com")
    }

    @Test
    fun emptyInput_doesNotCallMemberService() {
        assertThat(lookup.byUserIds(emptyList())).isEmpty()
        verifyNoInteractions(client)
    }

    @Test
    fun memberServiceDown_answersEmpty() {
        whenever(client.getMembersByUserId(any())).thenThrow(RuntimeException("member-service down"))
        whenever(client.searchMembers(any(), any(), any())).thenThrow(RuntimeException("member-service down"))

        assertThat(lookup.byUserIds(listOf("a"))).isEmpty()
        assertThat(lookup.search("앨리")).isEmpty()
    }

    @Test
    fun search_asksFirstPageOnly() {
        whenever(client.searchMembers(eq("앨리"), eq(0), eq(FeignMemberLookup.MAX_MATCHES)))
            .thenReturn(MemberPageDto(listOf(MemberSummaryDto("a", "앨리스", "alice@example.com"))))

        assertThat(lookup.search("앨리").map { it.userId }).containsExactly("a")
    }
}
