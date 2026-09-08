package com.example.modumessenger.Global;

import com.example.modumessenger.entity.Member;

import java.util.List;

/**
 * 채팅 말풍선의 보낸 사람을 참여자 목록에서 찾는다.
 *
 * 채팅방 화면은 참여자 목록(방 정보 API)과 채팅 내역(로컬 캐시)을 따로 불러온다.
 * 이미 열어 본 방은 캐시된 내역이 참여자 목록보다 먼저 도착하므로, 그 순간에는
 * 보낸 사람을 찾을 수 없다. 방을 나간 사람의 옛 메시지도 마찬가지다.
 * 이때 null 을 돌려주면 말풍선을 그리다 앱이 죽으므로, 자리표시자를 돌려준다.
 * 참여자 목록이 도착하면 어댑터가 다시 그려서 실제 이름과 사진으로 바뀐다.
 */
public class ChatSenderUtil {

    public static final String UNKNOWN_NAME = "알 수 없음";

    /** 절대 null 을 돌려주지 않는다. 못 찾으면 {@link #isKnown} 이 false 인 자리표시자다. */
    public static Member resolve(List<Member> members, String sender) {
        if (members != null && sender != null) {
            for (Member member : members) {
                if (sender.equals(member.getUserId())) {
                    return member;
                }
            }
        }
        return new Member(sender, "", UNKNOWN_NAME, "", "", "");
    }

    /** 서버에서 받은 실제 회원인지. 자리표시자는 id 가 없다. */
    public static boolean isKnown(Member member) {
        return member != null && member.getId() != null;
    }
}
