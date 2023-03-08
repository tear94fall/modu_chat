package com.example.modumessenger.common.data.controller;

import com.example.modumessenger.common.data.dto.CommonDataDto;
import com.example.modumessenger.common.data.service.CommonDataService;
import com.example.modumessenger.common.data.type.EnumMapperFactory;
import com.example.modumessenger.common.data.type.EnumMapperValue;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.*;

@RestController
@RequiredArgsConstructor
public class CommonDataController {

    private final CommonDataService commonDataService;
    private final EnumMapperFactory enumMapperFactory;

    @GetMapping("/value")
    public Map<String, List<EnumMapperValue>> getEnumValue() {
        return enumMapperFactory.getFactory();
    }

    @GetMapping("common/{key}")
    public ResponseEntity<CommonDataDto> getCommonData(@Valid @PathVariable("key") String key) {

//        enumMapperFactory.get("commonData").forEach(type->{
//            if(type.getTitle().equals(key)) {
//
//            }
//        });

        switch(key) {
            case "version": return ResponseEntity.ok().body(commonDataService.getVersion());
            case "locale": return ResponseEntity.ok().body(commonDataService.getLocale());
            case "time": return ResponseEntity.ok().body(commonDataService.getTime());
            default: return ResponseEntity.ok().body(new CommonDataDto("empty", "empty"));
        }
    }

    @GetMapping("commons/{key}")
    public ResponseEntity<List<CommonDataDto>> getCommonDataList(@Valid @PathVariable("key") String key) {
        switch(key) {
            case "notification": return ResponseEntity.ok().body(commonDataService.getNotification());
            default: return ResponseEntity.ok().body(new ArrayList<>(List.of(new CommonDataDto("empty", "empty"))));
        }
    }
}
