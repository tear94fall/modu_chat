package com.example.wsservice.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/** chat-service 의 집계와 같은 모양. 그대로 앱에 내려간다. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionSummaryDto implements Serializable {
    private String emoji;
    private int count;
    private List<String> userIds;
}
