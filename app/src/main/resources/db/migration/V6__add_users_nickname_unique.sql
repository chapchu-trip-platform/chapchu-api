-- 닉네임 중복 방지 (docs/decisions/027)
-- 애플리케이션 레이어에서 먼저 검사하지만, 동시 요청 경합(check-then-act)까지 막으려면 DB 제약이 필요하다.
-- PostgreSQL은 UNIQUE 인덱스에서 NULL을 서로 다른 값으로 취급하므로,
-- 아직 닉네임을 등록하지 않은 유저(nickname IS NULL)가 여럿 있어도 문제없다.
CREATE UNIQUE INDEX uk_users_nickname ON users (nickname);
