-- 지역 스탬프 도감 (이슈 #250)
--
-- 시·도 단위로 스탬프를 하나씩 둔다. 코스에 담긴 장소를 방문 인증하면 그 장소가 속한
-- 시·도의 스탬프가 발급되고, 재방문하면 횟수만 올라간다.
--
-- 지역 판정은 TourAPI areaCode(1=서울 … 39=제주)로 한다. 주소 앞부분을 파싱하면
-- '강원' vs '강원특별자치도'처럼 표기가 바뀌는 순간 스탬프가 조용히 안 나온다.

-- ── 지역에 TourAPI areaCode를 붙인다 ───────────────────────────────────
ALTER TABLE regions
    ADD COLUMN IF NOT EXISTS area_code SMALLINT;

UPDATE regions SET area_code = v.code
FROM (VALUES
    ('서울', 1), ('인천', 2), ('대전', 3), ('대구', 4), ('광주', 5),
    ('부산', 6), ('울산', 7), ('세종', 8), ('경기', 31), ('강원', 32),
    ('충북', 33), ('충남', 34), ('경북', 35), ('경남', 36), ('전북', 37),
    ('전남', 38), ('제주', 39)
) AS v(name, code)
WHERE regions.region_name = v.name;

ALTER TABLE regions
    ADD CONSTRAINT uq_regions_area_code UNIQUE (area_code);

COMMENT ON COLUMN regions.area_code IS 'TourAPI areaCode. 장소 → 지역 판정에 쓴다';

-- ── 장소가 어느 지역인지 ──────────────────────────────────────────────
ALTER TABLE places
    ADD COLUMN IF NOT EXISTS area_code SMALLINT;

CREATE INDEX IF NOT EXISTS idx_places_area_code ON places(area_code);

COMMENT ON COLUMN places.area_code IS 'TourAPI areaCode. 동기화 시 채운다. NULL이면 지역 미상';

-- ── 스탬프 = 지역 1:1 ─────────────────────────────────────────────────
ALTER TABLE stamps
    ADD COLUMN IF NOT EXISTS region_id UUID REFERENCES regions(region_id),
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

ALTER TABLE stamps
    ADD CONSTRAINT uq_stamps_region_id UNIQUE (region_id);

COMMENT ON COLUMN stamps.region_id IS '스탬프가 대응하는 시·도. FK → regions';
COMMENT ON COLUMN stamps.image_url IS '지자체 마스코트 이미지 URL. 준비되면 채운다';

INSERT INTO stamps (stamp_id, stamp_name, region_id, created_at, updated_at)
SELECT gen_random_uuid(), r.region_name || ' 스탬프', r.region_id, now(), now()
FROM regions r
WHERE r.area_code IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM stamps s WHERE s.region_id = r.region_id);

-- ── 사용자 보유 현황 ──────────────────────────────────────────────────
-- 같은 스탬프가 여러 행으로 쌓이면 횟수 집계가 어긋난다. 한 유저당 한 행으로 묶는다.
DELETE FROM user_stamps a
USING user_stamps b
WHERE a.user_id = b.user_id
  AND a.stamp_id = b.stamp_id
  AND a.user_stamp_id > b.user_stamp_id;

ALTER TABLE user_stamps
    ADD CONSTRAINT uq_user_stamps_user_stamp UNIQUE (user_id, stamp_id);

ALTER TABLE user_stamps
    ADD COLUMN IF NOT EXISTS first_acquired_at TIMESTAMP;

UPDATE user_stamps SET first_acquired_at = created_at WHERE first_acquired_at IS NULL;

COMMENT ON COLUMN user_stamps.first_acquired_at IS '처음 획득한 시각. 재방문해도 바뀌지 않는다';
