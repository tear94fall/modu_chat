rootProject.name = "member-service"

include("member-api")
include("member-application")
// @ApiLock / @LockParam 컴파일 시점 검증기(자바 애노테이션 프로세서). member-application 이 kapt 로 돌린다.
include("lock-processor")
