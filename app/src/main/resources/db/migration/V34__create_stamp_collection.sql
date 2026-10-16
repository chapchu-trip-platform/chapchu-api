-- 지역 스탬프 도감 — 9개 도 단위 (이슈 #250)
--
-- 스탬프는 9개 도(경기·강원·충북·충남·전북·전남·경북·경남·제주)로 둔다.
-- 코스에 담긴 장소를 방문 인증하면 그 장소가 속한 도의 스탬프가 발급되고,
-- 재방문하면 stamp_count만 올라간다.
--
-- 지역 판정은 TourAPI areaCode로 한다. 주소 문자열을 파싱하면 '강원'과
-- '강원특별자치도'처럼 표기가 바뀌는 순간 스탬프가 조용히 안 나온다.
--
-- 특별시·광역시(서울·부산·인천·대구·광주·대전·울산·세종)는 도가 아니라 스탬프가 없다.
-- 그 지역에서 방문 인증하면 스탬프는 발급되지 않고 방문 인증만 정상 처리된다.
--
-- 나중에 광역시도 인접 도로 보내고 싶으면 stamp_area_codes에 행만 넣으면 된다.
-- 코드는 건드릴 필요가 없다.

-- ── 장소가 어느 지역인지 ──────────────────────────────────────────────
ALTER TABLE places
    ADD COLUMN IF NOT EXISTS area_code SMALLINT;

CREATE INDEX IF NOT EXISTS idx_places_area_code ON places(area_code);

COMMENT ON COLUMN places.area_code IS 'TourAPI areaCode. 동기화 시 채운다. NULL이면 지역 미상';

-- ── 스탬프 9개 ────────────────────────────────────────────────────────
ALTER TABLE stamps
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(500);

ALTER TABLE stamps
    ADD CONSTRAINT uq_stamps_stamp_name UNIQUE (stamp_name);

COMMENT ON COLUMN stamps.image_url IS '도 마스코트 이미지 URL. 준비되면 채운다';

INSERT INTO stamps (stamp_id, stamp_name, created_at, updated_at)
SELECT gen_random_uuid(), v.name, now(), now()
FROM (VALUES
    ('경기'), ('강원'), ('충북'), ('충남'),
    ('전북'), ('전남'), ('경북'), ('경남'), ('제주')
) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM stamps s WHERE s.stamp_name = v.name);

-- ── areaCode → 스탬프 매핑 ────────────────────────────────────────────
-- 9개 도만 넣는다. 여기 없는 areaCode(광역시)는 스탬프가 발급되지 않는다.
CREATE TABLE IF NOT EXISTS stamp_area_codes (
    area_code SMALLINT PRIMARY KEY,
    stamp_id  UUID NOT NULL REFERENCES stamps(stamp_id) ON DELETE CASCADE
);

COMMENT ON TABLE stamp_area_codes IS 'TourAPI areaCode → 스탬프. 9개 도만 등록된다';

INSERT INTO stamp_area_codes (area_code, stamp_id)
SELECT v.code, s.stamp_id
FROM (VALUES
    (31, '경기'), (32, '강원'), (33, '충북'), (34, '충남'),
    (35, '경북'), (36, '경남'), (37, '전북'), (38, '전남'), (39, '제주')
) AS v(code, name)
JOIN stamps s ON s.stamp_name = v.name
ON CONFLICT (area_code) DO NOTHING;

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
