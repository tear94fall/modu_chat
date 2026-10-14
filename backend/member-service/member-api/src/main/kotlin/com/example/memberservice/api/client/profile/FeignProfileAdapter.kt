package com.example.memberservice.api.client.profile

import com.example.memberservice.api.dto.ProfileDto
import com.example.memberservice.application.port.ProfileInfo
import com.example.memberservice.application.port.ProfilePort
import org.springframework.stereotype.Component

/** [ProfilePort] 의 Feign 구현. 조회는 Feign 클라이언트의 재시도·폴백(빈 목록)을 그대로 쓴다. */
@Component
class FeignProfileAdapter(private val profileFeignClient: ProfileFeignClient) : ProfilePort {

    override fun getMemberProfiles(memberId: Long): List<ProfileInfo>? =
        profileFeignClient.getMemberProfiles(memberId).body?.map { it.toInfo() }

    override fun addProfile(profile: ProfileInfo): ProfileInfo? =
        profileFeignClient.addProfileRequest(ProfileDto.of(profile)).body?.toInfo()
}
