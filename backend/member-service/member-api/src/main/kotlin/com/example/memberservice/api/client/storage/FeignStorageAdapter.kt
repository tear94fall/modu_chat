package com.example.memberservice.api.client.storage

import com.example.memberservice.application.port.StoragePort
import org.springframework.stereotype.Component

/** [StoragePort] 의 Feign 구현. */
@Component
class FeignStorageAdapter(private val storageFeignClient: StorageFeignClient) : StoragePort {

    override fun uploadFromUrl(url: String): String? = storageFeignClient.upload(url).body

    override fun delete(file: String) {
        storageFeignClient.delete(file)
    }
}
