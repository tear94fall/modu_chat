-- ShedLock 락 테이블. 운영 DB 에는 DBA 가 같은 DDL 로 만든다(modu_infra/data/mysql/schema). 엔티티가 아니라 Hibernate 가 안 만들어 테스트는 여기서 만든다.
CREATE TABLE IF NOT EXISTS shedlock (
  name VARCHAR(64) NOT NULL,
  lock_until TIMESTAMP(3) NOT NULL,
  locked_at TIMESTAMP(3) NOT NULL,
  locked_by VARCHAR(255) NOT NULL,
  PRIMARY KEY (name)
);
