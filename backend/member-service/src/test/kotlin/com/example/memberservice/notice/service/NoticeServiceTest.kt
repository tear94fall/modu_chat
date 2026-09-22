package com.example.memberservice.notice.service

import com.example.memberservice.member.entity.Member
import com.example.memberservice.member.repository.MemberRepository
import com.example.memberservice.notice.client.PushFeignClient
import com.example.memberservice.notice.dto.CreateNoticeDto
import com.example.memberservice.notice.dto.PushMessageDto
import com.example.memberservice.notice.repository.NoticeRepository
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean

@SpringBootTest
class NoticeServiceTest {

    @Autowired lateinit var noticeService: NoticeService
    @Autowired lateinit var noticeRepository: NoticeRepository
    @Autowired lateinit var memberRepository: MemberRepository
    @MockitoBean lateinit var pushFeignClient: PushFeignClient

    private fun request(title: String, content: String, push: Boolean): CreateNoticeDto {
        val dto = CreateNoticeDto()
        dto.title = title
        dto.content = content
        dto.push = push
        return dto
    }

    private fun admin(username: String): Member {
        val unique = UUID.randomUUID().toString()
        return memberRepository.save(Member(userId = unique, email = "$unique@modu.com", username = username))
    }

    @Test
    fun createNotice_savesAndBroadcasts() {
        val created = noticeService.createNotice(request("점검 안내", "오늘 밤 점검이 있습니다", true), null)

        assertThat(created.id).isNotNull()
        assertThat(created.title).isEqualTo("점검 안내")
        assertThat(created.createdDate).isNotNull()
        assertThat(noticeRepository.findById(created.id!!)).isPresent
        verify(pushFeignClient).broadcast(any<PushMessageDto>())
    }

    @Test
    fun createNotice_withoutPush_doesNotBroadcast() {
        noticeService.createNotice(request("조용한 공지", "알림 없이 올립니다", false), null)

        verify(pushFeignClient, never()).broadcast(any<PushMessageDto>())
    }

    /** 푸시가 실패해도 글은 남아야 한다 — 알림을 놓쳐도 공지사항에서 다시 볼 수 있어야 하기 때문이다. */
    @Test
    fun createNotice_keepsNoticeWhenPushFails() {
        whenever(pushFeignClient.broadcast(any<PushMessageDto>())).thenThrow(RuntimeException("push down"))

        val created = noticeService.createNotice(request("푸시 실패", "그래도 남는다", true), null)

        assertThat(noticeRepository.findById(created.id!!)).isPresent
    }

    @Test
    fun getNotices_newestFirst() {
        noticeService.createNotice(request("첫 번째", "1", false), null)
        noticeService.createNotice(request("두 번째", "2", false), null)

        val notices = noticeService.getNotices()

        assertThat(notices).isNotEmpty
        assertThat(notices[0].title).isEqualTo("두 번째")
    }

    /** 작성자는 게이트웨이가 넘긴 userId 로 회원을 찾아 그 시점 이름을 박아 둔다. */
    @Test
    fun createNotice_recordsWriterNameFromUserId() {
        val writer = admin("운영팀 임준섭")

        val created = noticeService.createNotice(request("작성자 확인", "누가 올렸는지 남는다", false), writer.userId)

        assertThat(created.writer).isEqualTo("운영팀 임준섭")
        assertThat(noticeRepository.findById(created.id!!).orElseThrow().writerId).isEqualTo(writer.userId)
    }

    /** userId 가 없거나(게이트웨이를 안 거친 호출) 회원을 못 찾아도 저장은 막지 않는다. */
    @Test
    fun createNotice_fallsBackToDefaultWriter() {
        assertThat(noticeService.createNotice(request("헤더 없음", "본문", false), null).writer).isEqualTo("관리자")
        assertThat(noticeService.createNotice(request("모르는 회원", "본문", false), "없는-사용자").writer).isEqualTo("관리자")
    }
}
