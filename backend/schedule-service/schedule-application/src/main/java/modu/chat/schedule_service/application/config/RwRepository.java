package modu.chat.schedule_service.application.config;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

/** 읽기/쓰기(master) 저장소 마커. */
@NoRepositoryBean
public interface RwRepository<T, ID> extends JpaRepository<T, ID> {
}
