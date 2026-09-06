package com.example.memberservice.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.notice.client.PushFeignClient;
import com.example.memberservice.notice.dto.CreateNoticeDto;
import com.example.memberservice.notice.dto.NoticeDto;
import com.example.memberservice.notice.dto.PushMessageDto;
import com.example.memberservice.notice.repository.NoticeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.UUID;

@SpringBootTest
class NoticeServiceTest {

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private MemberRepository memberRepository;

    @MockBean
    private PushFeignClient pushFeignClient;

    private CreateNoticeDto request(String title, String content, boolean push) {
        CreateNoticeDto dto = new CreateNoticeDto();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setPush(push);
        return dto;
    }

    private Member admin(String username) {
        String unique = UUID.randomUUID().toString();
        return memberRepository.save(Member.builder()
                .userId(unique)
                .email(unique + "@modu.com")
                .username(username)
                .build());
    }

    @Test
    void createNotice_savesAndBroadcasts() {
        NoticeDto created = noticeService.createNotice(request("점검 안내", "오늘 밤 점검이 있습니다", true), null);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getTitle()).isEqualTo("점검 안내");
        assertThat(created.getCreatedDate()).isNotNull();
        assertThat(noticeRepository.findById(created.getId())).isPresent();
        verify(pushFeignClient).broadcast(any(PushMessageDto.class));
    }

    @Test
    void createNotice_withoutPush_doesNotBroadcast() {
        noticeService.createNotice(request("조용한 공지", "알림 없이 올립니다", false), null);

        verify(pushFeignClient, org.mockito.Mockito.never()).broadcast(any(PushMessageDto.class));
    }

    /** 푸시가 실패해도 글은 남아야 한다 — 알림을 놓쳐도 공지사항에서 다시 볼 수 있어야 하기 때문이다. */
    @Test
    void createNotice_keepsNoticeWhenPushFails() {
        when(pushFeignClient.broadcast(any(PushMessageDto.class))).thenThrow(new RuntimeException("push down"));

        NoticeDto created = noticeService.createNotice(request("푸시 실패", "그래도 남는다", true), null);

        assertThat(noticeRepository.findById(created.getId())).isPresent();
    }

    @Test
    void getNotices_newestFirst() {
        noticeService.createNotice(request("첫 번째", "1", false), null);
        noticeService.createNotice(request("두 번째", "2", false), null);

        List<NoticeDto> notices = noticeService.getNotices();

        assertThat(notices).isNotEmpty();
        assertThat(notices.get(0).getTitle()).isEqualTo("두 번째");
    }

    /** 작성자는 게이트웨이가 넘긴 userId 로 회원을 찾아 그 시점 이름을 박아 둔다. */
    @Test
    void createNotice_recordsWriterNameFromUserId() {
        Member writer = admin("운영팀 임준섭");

        NoticeDto created = noticeService.createNotice(request("작성자 확인", "누가 올렸는지 남는다", false), writer.getUserId());

        assertThat(created.getWriter()).isEqualTo("운영팀 임준섭");
        assertThat(noticeRepository.findById(created.getId()).orElseThrow().getWriterId())
                .isEqualTo(writer.getUserId());
    }

    /** userId 가 없거나(게이트웨이를 안 거친 호출) 회원을 못 찾아도 저장은 막지 않는다. */
    @Test
    void createNotice_fallsBackToDefaultWriter() {
        assertThat(noticeService.createNotice(request("헤더 없음", "본문", false), null).getWriter())
                .isEqualTo("관리자");
        assertThat(noticeService.createNotice(request("모르는 회원", "본문", false), "없는-사용자").getWriter())
                .isEqualTo("관리자");
    }
}
