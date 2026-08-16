-- 선호 코드값 확장 (이슈 #112)
--
-- V3 시드가 최소한만 들어 있어 실제 서비스에 쓸 수 없었다.
--   지역 4곳 · 테마 4개 · 이동수단 3개
--
-- 이름에 UNIQUE를 걸어 앞으로 중복이 들어가지 않게 하고, ON CONFLICT로 기존 값과 겹쳐도 건너뛴다.
-- (ALTER TABLE 자체는 재실행 불가다. Flyway가 마이그레이션을 한 번만 돌리므로 문제되지 않는다.)

-- ============================================================
-- 1. 지역 — 17개 광역자치단체
-- ============================================================

ALTER TABLE regions ADD CONSTRAINT uq_regions_region_name UNIQUE (region_name);

INSERT INTO regions (region_id, region_name, created_at, updated_at) VALUES
  (gen_random_uuid(), '대구', now(), now()),
  (gen_random_uuid(), '인천', now(), now()),
  (gen_random_uuid(), '광주', now(), now()),
  (gen_random_uuid(), '대전', now(), now()),
  (gen_random_uuid(), '울산', now(), now()),
  (gen_random_uuid(), '세종', now(), now()),
  (gen_random_uuid(), '경기', now(), now()),
  (gen_random_uuid(), '충북', now(), now()),
  (gen_random_uuid(), '충남', now(), now()),
  (gen_random_uuid(), '전북', now(), now()),
  (gen_random_uuid(), '전남', now(), now()),
  (gen_random_uuid(), '경북', now(), now()),
  (gen_random_uuid(), '경남', now(), now())
ON CONFLICT (region_name) DO NOTHING;
-- 서울 · 부산 · 제주 · 강원은 V3에 이미 있다.

-- ============================================================
-- 2. 이동수단 — 자전거 추가
-- ============================================================

ALTER TABLE transport_methods
  ADD CONSTRAINT uq_transport_methods_name UNIQUE (transport_method_name);

INSERT INTO transport_methods (transport_method_id, transport_method_name, created_at, updated_at)
VALUES (gen_random_uuid(), '자전거', now(), now())
ON CONFLICT (transport_method_name) DO NOTHING;

-- ============================================================
-- 3. 테마 — TourAPI contentTypeId 기준으로 교체
-- ============================================================
--
-- 기존 테마(카페·공원·해변·숙박)는 분류 축이 섞여 있었다. 카페는 시설, 해변은 지형이다.
-- 장소 출처가 TourAPI이므로 대분류에 맞추면 유저 선호와 장소 분류가 같은 축이 된다.
--
-- content_type_id 를 함께 저장해야 나중에 장소를 받아올 때 places.theme_id 를 채울 수 있다.
-- 지금은 그 컬럼을 채우는 코드가 없어 항상 null이다.

ALTER TABLE themes ADD COLUMN content_type_id INT;

-- user_preference_themes.theme_id 에는 ON DELETE CASCADE 가 없다.
-- 옛 테마를 참조하는 선호가 남아 있으면 아래 DELETE 가 FK 위반으로 실패하므로 먼저 정리한다.
DELETE FROM user_preference_themes
 WHERE theme_id IN (SELECT theme_id FROM themes WHERE content_type_id IS NULL);

DELETE FROM themes WHERE content_type_id IS NULL;

ALTER TABLE themes ADD CONSTRAINT uq_themes_content_type_id UNIQUE (content_type_id);
ALTER TABLE themes ADD CONSTRAINT uq_themes_theme_name UNIQUE (theme_name);

INSERT INTO themes (theme_id, theme_name, content_type_id, created_at, updated_at) VALUES
  (gen_random_uuid(), '관광지',       12, now(), now()),
  (gen_random_uuid(), '문화시설',     14, now(), now()),
  (gen_random_uuid(), '축제공연행사', 15, now(), now()),
  (gen_random_uuid(), '여행코스',     25, now(), now()),
  (gen_random_uuid(), '레포츠',       28, now(), now()),
  (gen_random_uuid(), '숙박',         32, now(), now()),
  (gen_random_uuid(), '쇼핑',         38, now(), now()),
  (gen_random_uuid(), '음식점',       39, now(), now())
ON CONFLICT (content_type_id) DO NOTHING;

ALTER TABLE themes ALTER COLUMN content_type_id SET NOT NULL;
