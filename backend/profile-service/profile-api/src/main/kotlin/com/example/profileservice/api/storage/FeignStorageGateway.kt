package com.example.profileservice.api.storage

import com.example.profileservice.application.port.StorageGateway
import org.springframework.stereotype.Component

/** [StorageGateway] 의 Feign 구현. */
@Component
class FeignStorageGateway(private val storageFeignClient: StorageFeignClient) : StorageGateway {

    override fun delete(file: String) {
        storageFeignClient.delete(file)
    }
}
