-- users.location_consent — 위치 정보 수집 동의 여부 (이슈 #130)
--
-- Entity(User.locationConsent)와 docs/schema/init.sql에는 이미 컬럼이 있지만
-- Flyway 마이그레이션이 빠져 있었다. docs/schema/init.sql은 어디에서도 실행되지
-- 않는 문서일 뿐이고, 실제 스키마는 이 디렉터리가 전부다.
--
-- ddl-auto=validate라 이 파일이 없으면 배포된 DB에 컬럼이 없는 채로
-- Hibernate 스키마 검증이 실패해 앱이 아예 뜨지 않는다.
--
-- 기본값 TRUE: 이미 가입한 유저는 동의한 것으로 본다. 컬럼을 추가하는 시점에
-- 되물을 방법이 없고, 그동안 위치 기반 기능(GET /places/nearby)을 써 왔다.
--
-- IF NOT EXISTS: 개발 DB에 이 DDL을 손으로 이미 적용했을 수 있다.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS location_consent BOOLEAN NOT NULL DEFAULT TRUE;
