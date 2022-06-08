package com.example.modumessenger.common.data.controller;

import com.example.modumessenger.common.data.dto.CommonDataDto;
import com.example.modumessenger.common.data.service.CommonDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CommonDataController {

    private final CommonDataService commonDataService;

    @GetMapping("/common/version")
    public ResponseEntity<CommonDataDto> chatRoomList() {
        CommonDataDto commonDataDto = commonDataService.getVersion();
        return ResponseEntity.ok().body(commonDataDto);
    }
}
