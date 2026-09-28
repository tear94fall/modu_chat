package modu.chat.schedule_service.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import modu.chat.schedule_service.schedule.entity.DataType;
import modu.chat.schedule_service.schedule.entity.Protocol;
import modu.chat.schedule_service.schedule.entity.Schedule;

@Schema(description = "스케줄. 등록 요청과 응답에 같이 쓴다. 주기 변경(PATCH)은 cronExpression 만 읽는다.")
@Getter
@Setter
@NoArgsConstructor
public class ScheduleDto {

    @Schema(description = "스케줄 id. 서버가 매기므로 등록 때는 비운다", example = "1")
    private Long id;
    @Schema(description = "스케줄 이름. cronExpression 과 함께 중복 등록을 가리는 키", example = "daily-report")
    private String name;
    @Schema(description = "호출할 호스트. https://address:port 로 부른다", example = "example.com")
    private String address;
    @Schema(description = "호출 경로", example = "/api/report")
    private String path;
    @Schema(description = "호출 방식. REST_API 만 실제로 호출하고 나머지는 로그만 남긴다", example = "REST_API")
    private Protocol protocol;
    @Schema(description = "HTTP 메서드. GET 또는 POST 만 호출한다", example = "GET")
    private String method;
    @Schema(description = "호출 포트", example = "443")
    private int port;
    @Schema(description = "dataValue 해석 방식(GET 기준). STRING 은 path 뒤에 붙이고, JSON 은 쿼리 파라미터로 펼치고, NONE 은 쓰지 않는다. POST 는 dataValue 를 본문으로 보낸다", example = "JSON")
    private DataType dataType;
    @Schema(description = "호출에 실을 값. dataType 에 따라 경로 조각·JSON 객체·POST 본문", example = "{\"date\":\"today\"}")
    private String dataValue;
    @Schema(description = "Spring cron 식(초 분 시 일 월 요일, 6자리). 필수", example = "0 0 9 * * *")
    private String cronExpression;
    @Schema(description = "스케줄 설명(작업 실행 로그에 남는다)", example = "매일 오전 9시 리포트 호출")
    private String description;

    @Builder
    public ScheduleDto(Long id, String name, String address, String path, Protocol protocol, String method, int port, DataType dataType, String dataValue, String cronExpression, String description) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.path = path;
        this.protocol = protocol;
        this.method = method;
        this.port = port;
        this.dataType = dataType;
        this.dataValue = dataValue;
        this.cronExpression = cronExpression;
        this.description = description;
    }

    public static ScheduleDto from(Schedule schedule) {
        return ScheduleDto
                .builder()
                .id(schedule.getId())
                .name(schedule.getName())
                .address(schedule.getAddress())
                .path(schedule.getPath())
                .protocol(schedule.getProtocol())
                .method(schedule.getMethod())
                .port(schedule.getPort())
                .dataType(schedule.getDataType())
                .dataValue(schedule.getDataValue())
                .cronExpression(schedule.getCronExpression())
                .description(schedule.getDescription())
                .build();
    }
}
