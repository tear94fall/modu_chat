package com.example.pushservice.fcm.repository;

import com.example.pushservice.fcm.entity.FcmToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FcmRepository extends JpaRepository<FcmToken, Long> {

    /** userId 당 여러 행이 남아있을 수 있어(과거 버그) 가장 최근 것 하나만 가져온다. */
    Optional<FcmToken> findFirstByUserIdOrderByIdDesc(String userId);

    /** upsert 후 같은 userId 의 나머지(오래된) 중복 행을 정리한다. */
    void deleteByUserIdAndIdNot(String userId, Long id);
}
