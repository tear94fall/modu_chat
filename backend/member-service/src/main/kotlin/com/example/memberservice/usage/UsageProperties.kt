package com.example.memberservice.usage

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * modu.member.* — usage-clients 는 OAuth 클라이언트 ID → 서비스. auth-service 의 등록 클라이언트
 * (config-repo messenger.yml 의 modu.oauth.clients) 와 같은 ID 를 쓴다. 여기 없는 클라이언트(직원 콘솔 등)는 기록하지 않는다.
 */
@ConfigurationProperties(prefix = "modu.member")
class UsageProperties {
    var usageClients: MutableMap<String, ModuService> = mutableMapOf()
}
