package com.example.chatservice.application.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.data.redis.RedisProperties

/**
 * spring.data.redis.cluster.nodes 유무에 따라 Redisson 이 클러스터/단일 서버 모드로 갈라지는지 확인한다.
 * 운영(dev k8s)은 config-repo 의 클러스터 노드, IDE 실행(local 프로필)은 단일 서버다.
 */
class RedissonConfigTest {

    @Test
    fun clusterNodesPresent_usesClusterServers() {
        val props = RedisProperties()
        props.cluster = RedisProperties.Cluster()
        props.cluster.nodes = listOf("redis-node-1:7001", "redis-node-2:7002")

        val config = RedissonConfig.buildConfig(props)

        assertThat(config.isClusterConfig).isTrue()
        assertThat(config.useClusterServers().nodeAddresses)
            .containsExactly("redis://redis-node-1:7001", "redis://redis-node-2:7002")
    }

    @Test
    fun clusterNodesAbsent_usesSingleServer() {
        val props = RedisProperties()
        props.host = "localhost"
        props.port = 6379

        val config = RedissonConfig.buildConfig(props)

        assertThat(config.isClusterConfig).isFalse()
        assertThat(config.useSingleServer().address).isEqualTo("redis://localhost:6379")
    }

    @Test
    fun clusterNodesEmpty_usesSingleServer() {
        val props = RedisProperties()
        props.host = "localhost"
        props.port = 6379
        props.cluster = RedisProperties.Cluster()
        props.cluster.nodes = listOf()

        val config = RedissonConfig.buildConfig(props)

        assertThat(config.isClusterConfig).isFalse()
    }
}
