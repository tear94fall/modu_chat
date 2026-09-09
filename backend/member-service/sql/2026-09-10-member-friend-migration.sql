-- 친구 별칭 도입: friends 원소 컬렉션 → member_friend 테이블.
-- 절차: (1) 새 member-service 배포로 member_friend 가 생성된 뒤 (2) 이 파일의 1단계를 실행하고
--       (3) 앱에서 친구 목록이 그대로 보이는지 확인한 뒤 (4) 2단계로 옛 테이블을 지운다.
-- mysql 클라이언트는 --default-character-set=utf8mb4 로 실행한다(한글 별칭 복사).

-- 1단계: 옛 friends(id=나, friends=친구) 를 옮긴다. 별칭 초기값은 친구의 현재 username.
INSERT IGNORE INTO member_friend (member_id, friend_member_id, friend_name, created_date)
SELECT f.id, f.friends, COALESCE(m.username, ''), NOW(6)
FROM friends f
JOIN member m ON m.member_id = f.friends;

-- 검증: 두 수가 같아야 한다(중복 행이 있었다면 member_friend 가 더 작다).
SELECT (SELECT COUNT(DISTINCT id, friends) FROM friends) AS old_pairs,
       (SELECT COUNT(*) FROM member_friend) AS new_rows;

-- 2단계(검증 후 별도 실행): 죽은 엔티티가 만든 테이블과 옛 컬렉션 테이블 제거.
-- DROP TABLE IF EXISTS friend_friends, friend, favorite, block, hidden, friends;
