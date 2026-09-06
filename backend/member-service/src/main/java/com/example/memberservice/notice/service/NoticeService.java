package com.example.memberservice.notice.service;

import com.example.memberservice.member.repository.MemberRepository;
import com.example.memberservice.notice.client.PushFeignClient;
import com.example.memberservice.notice.dto.CreateNoticeDto;
import com.example.memberservice.notice.dto.NoticeDto;
import com.example.memberservice.notice.dto.NoticeWriter;
import com.example.memberservice.notice.dto.PushMessageDto;
import com.example.memberservice.notice.entity.Notice;
import com.example.memberservice.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final MemberRepository memberRepository;
    private final PushFeignClient pushFeignClient;

    /** 앱 공지사항 목록. 최신 글이 위로 온다. */
    @Transactional(readOnly = true)
    public List<NoticeDto> getNotices() {
        return noticeRepository.findAllByOrderByIdDesc().stream().map(NoticeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<NoticeDto> searchNotices(Pageable pageable) {
        return noticeRepository.findAllByOrderByIdDesc(pageable).map(NoticeDto::from);
    }

    @Transactional(readOnly = true)
    public NoticeDto getNotice(Long id) {
        return noticeRepository.findById(id).map(NoticeDto::from).orElse(null);
    }

    /**
     * 공지를 저장하고, 요청이면 전체 푸시로도 내보낸다.
     * 푸시가 실패해도 공지는 남긴다 — 글이 사라지는 것보다 알림이 못 간 편이 낫고,
     * 알림을 놓쳐도 사용자가 공지사항에서 다시 볼 수 있다.
     *
     * @param writerUserId 게이트웨이가 관리자 JWT 를 검증하고 넣어 주는 userId.
     *                     클라이언트가 직접 채울 수 없는 값이라 작성자를 이걸로 정한다.
     */
    @Transactional
    public NoticeDto createNotice(CreateNoticeDto request, String writerUserId) {
        Notice saved = noticeRepository.save(
                new Notice(request.getTitle(), request.getContent(), writerUserId, resolveWriterName(writerUserId)));

        if (request.isPush()) {
            try {
                pushFeignClient.broadcast(new PushMessageDto(
                        request.getTitle(), request.getContent(),
                        Map.of("type", "notice", "noticeId", String.valueOf(saved.getId())), null));
            } catch (Exception e) {
                log.warn("공지 {} 저장은 됐지만 푸시 발송에 실패했다: {}", saved.getId(), e.getMessage());
            }
        }

        return NoticeDto.from(saved);
    }

    /** 회원을 못 찾아도 저장은 막지 않는다. 이름 하나 때문에 공지가 안 올라가면 곤란하다. */
    private String resolveWriterName(String writerUserId) {
        if (!StringUtils.hasText(writerUserId)) {
            return NoticeWriter.DEFAULT_NAME;
        }
        return memberRepository.findByUserId(writerUserId)
                .map(member -> StringUtils.hasText(member.getUsername())
                        ? member.getUsername() : NoticeWriter.DEFAULT_NAME)
                .orElse(NoticeWriter.DEFAULT_NAME);
    }
}
