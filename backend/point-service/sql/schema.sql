-- point-service 는 mysql-member 인스턴스의 별도 스키마를 쓴다(채팅 DB 와 조인하지 않는다).
-- 테이블은 JPA(ddl-auto: update)가 만든다. 스키마만 한 번 만들어 두면 된다.
CREATE DATABASE IF NOT EXISTS `modu-point` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
