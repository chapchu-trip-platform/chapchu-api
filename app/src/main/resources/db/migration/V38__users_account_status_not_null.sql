-- account_status는 DEFAULT 'ACTIVE'만 있고 NOT NULL이 아니었다.
-- DEFAULT는 INSERT에서 컬럼을 생략했을 때만 적용되므로 명시적 NULL이 들어갈 수 있었다.
-- 탈퇴 계정 차단(docs/decisions/049)이 매 요청 이 컬럼을 보기 때문에 값이 항상 있도록 못 박는다.
UPDATE users SET account_status = 'ACTIVE' WHERE account_status IS NULL;

ALTER TABLE users ALTER COLUMN account_status SET DEFAULT 'ACTIVE';
ALTER TABLE users ALTER COLUMN account_status SET NOT NULL;
