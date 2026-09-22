package com.example.memberservice.config

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import redis.embedded.RedisServer

@Configuration
class RedisEmbeddedConfig(
    @Value("\${spring.data.redis.host}") private val host: String,
    @Value("\${spring.data.redis.port}") private val port: Int,
) {

    private val log = LoggerFactory.getLogger(RedisEmbeddedConfig::class.java)
    private var redisServer: RedisServer? = null

    @PostConstruct
    @Throws(IOException::class)
    fun start() {
        val redisPort = if (isRedisRunning()) findAvailablePort() else port

        log.info("Starting redis server on port {}", redisPort)

        redisServer = RedisServer(redisPort).also { it.start() }
    }

    @PreDestroy
    @Throws(IOException::class)
    fun stop() {
        redisServer?.stop()
    }

    @Throws(IOException::class)
    private fun isRedisRunning(): Boolean = isRunning(executeGrepProcessCommand(port))

    @Throws(IOException::class)
    fun findAvailablePort(): Int {
        for (candidate in 10000..65535) {
            val process = executeGrepProcessCommand(candidate)
            if (!isRunning(process)) {
                return candidate
            }
        }
        throw IllegalArgumentException("Not Found Available port: 10000 ~ 65535")
    }

    @Throws(IOException::class)
    private fun executeGrepProcessCommand(port: Int): Process {
        val command = "netstat -nat | grep LISTEN|grep $port"
        val shell = arrayOf("/bin/sh", "-c", command)
        return Runtime.getRuntime().exec(shell)
    }

    private fun isRunning(process: Process): Boolean {
        val pidInfo = StringBuilder()
        try {
            BufferedReader(InputStreamReader(process.inputStream)).use { input ->
                var line = input.readLine()
                while (line != null) {
                    pidInfo.append(line)
                    line = input.readLine()
                }
            }
        } catch (e: Exception) {
            log.error(e.message)
        }
        return pidInfo.isNotEmpty()
    }
}
