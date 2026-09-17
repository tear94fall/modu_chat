package com.example.memberservice.member.dto;

import com.example.memberservice.global.lock.Lockable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** auth-service 가 구글 ID 토큰을 검증한 결과. 이메일로 회원을 찾거나 만든다. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAccountDto implements Lockable {
    private String sub;
    private String email;
    private String name;
    private String picture;

    /** 같은 계정의 동시 가입을 직렬화하는 락 키 */
    @Override
    public String getKey() {
        return "google-" + email;
    }
}
