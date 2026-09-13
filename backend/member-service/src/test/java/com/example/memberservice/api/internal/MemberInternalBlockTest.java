package com.example.memberservice.api.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.memberservice.member.entity.Member;
import com.example.memberservice.member.entity.MemberFriend;
import com.example.memberservice.member.repository.MemberFriendRepository;
import com.example.memberservice.member.repository.MemberRepository;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * chat-service / ws-service 가 부르는 차단 조회 내부 API.
 * 정방향(blocked-ids)과 역방향(blocked-by)이 서로 다른 답을 주는지까지 본다 —
 * 차단은 단방향이라 이 둘이 뒤바뀌면 엉뚱한 사람의 메시지가 사라진다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MemberInternalBlockTest {

    private static final String TOKEN = "test-internal-token";

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired MemberFriendRepository memberFriendRepository;

    private Member save(String userId) {
        return memberRepository.save(Member.builder()
                .userId(userId).email(userId + "@example.com").username(userId)
                .profiles(new ArrayList<>()).chatRoomMembers(new ArrayList<>())
                .build());
    }

    private MemberFriend befriend(Member me, Member friend) {
        return memberFriendRepository.save(MemberFriend.of(me, friend));
    }

    @Test
    void 차단한_사람만_blocked_ids_에_나온다() throws Exception {
        Member me = save("block-me");
        Member blocked = save("block-b1");
        Member hidden = save("block-h1");
        Member normal = save("block-n1");

        befriend(me, blocked).block();
        befriend(me, hidden).hide();
        befriend(me, normal);

        mockMvc.perform(get("/api-internal/member/{userId}/blocked-ids", me.getUserId()).header(InternalApiFilter.HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("block-b1"));
    }

    @Test
    void 차단을_풀면_blocked_ids_에서_빠진다() throws Exception {
        Member me = save("unblock-me");
        Member friend = save("unblock-f1");
        MemberFriend row = befriend(me, friend);
        row.block();
        row.unblock();

        mockMvc.perform(get("/api-internal/member/{userId}/blocked-ids", me.getUserId()).header(InternalApiFilter.HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void blocked_by_는_나를_차단한_사람을_주고_내가_차단한_사람은_주지_않는다() throws Exception {
        Member target = save("by-target");
        Member blocker = save("by-blocker");
        Member blockedByTarget = save("by-victim");
        Member notBlocking = save("by-friend");

        befriend(blocker, target).block();   // blocker → target 차단 (역방향에 나와야 한다)
        befriend(target, blockedByTarget).block(); // target 이 차단한 쪽 (역방향에 나오면 안 된다)
        befriend(notBlocking, target);       // 그냥 친구

        mockMvc.perform(get("/api-internal/member/{userId}/blocked-by", target.getUserId()).header(InternalApiFilter.HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("by-blocker"));

        mockMvc.perform(get("/api-internal/member/{userId}/blocked-ids", target.getUserId()).header(InternalApiFilter.HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0]").value("by-victim"));
    }

    @Test
    void 없는_userId_의_blocked_by_는_빈_목록이다() throws Exception {
        mockMvc.perform(get("/api-internal/member/{userId}/blocked-by", "no-such-user").header(InternalApiFilter.HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void 내부_토큰이_없으면_둘_다_거부한다() throws Exception {
        mockMvc.perform(get("/api-internal/member/x/blocked-ids")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api-internal/member/x/blocked-by")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api-internal/member/x/blocked-ids").header(InternalApiFilter.HEADER, "wrong"))
                .andExpect(status().isForbidden());
    }
}
