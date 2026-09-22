package com.example.memberservice.config

import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * spring.data.redis.cluster.nodes 가 있으면 Redis Cluster, 없으면 단일 서버(host/port)로 Redisson 을 붙인다.
 * 운영/로컬은 config-repo 가 클러스터 노드를 내려주고, 임베디드 Redis 를 쓰는 테스트 프로필은 단일 서버로 남는다.
 */
@Configuration
class RedissonConfig {

    @Bean
    fun redissonClient(redisProperties: RedisProperties): RedissonClient = Redisson.create(buildConfig(redisProperties))

    companion object {
        private const val REDISSON_HOST_PREFIX = "redis://"

        @JvmStatic
        fun buildConfig(redisProperties: RedisProperties): Config {
            val config = Config()

            val clusterNodes = redisProperties.cluster?.nodes

            if (clusterNodes.isNullOrEmpty()) {
                config.useSingleServer()
                    .setAddress(REDISSON_HOST_PREFIX + redisProperties.host + ":" + redisProperties.port)
                return config
            }

            val cluster = config.useClusterServers()
            clusterNodes.forEach { node -> cluster.addNodeAddress(REDISSON_HOST_PREFIX + node) }
            return config
        }
    }
}
