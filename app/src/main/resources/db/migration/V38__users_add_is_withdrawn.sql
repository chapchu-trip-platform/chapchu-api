-- 회원 탈퇴 플래그를 boolean으로 전환 (이슈 #302)
--
-- account_status(VARCHAR 'ACTIVE'/'WITHDRAWN')는 값이 둘뿐이라 문자열 enum을 유지할 이유가 없다.
-- is_withdrawn boolean으로 바꾼다.
--
-- account_status는 여기서 지우지 않는다. chapchu-auth가 같은 users 테이블을
-- ddl-auto: none 으로 매핑하고 있어, 두 서비스 배포 사이에 컬럼이 사라지면
-- 로그인 쿼리가 런타임에 깨진다. 양쪽이 is_withdrawn으로 전환된 뒤 별도 마이그레이션에서 DROP한다.

-- 검증(전): SELECT account_status, count(*) FROM users GROUP BY account_status;

ALTER TABLE users
    ADD COLUMN is_withdrawn BOOLEAN NOT NULL DEFAULT false;

-- 기존 행은 위 DEFAULT로 전부 false가 된다. 탈퇴 상태만 true로 올린다.
UPDATE users SET is_withdrawn = true WHERE account_status = 'WITHDRAWN';

COMMENT ON COLUMN users.is_withdrawn IS '탈퇴 여부. true면 토큰을 발급하지 않는다';

-- 검증(후): SELECT is_withdrawn, account_status, count(*) FROM users GROUP BY 1, 2 ORDER BY 1, 2;
--   기대: (false, 'ACTIVE') 다수 / (true, 'WITHDRAWN') 탈퇴자 수. 그 외 조합이 나오면 백필 오류다.
