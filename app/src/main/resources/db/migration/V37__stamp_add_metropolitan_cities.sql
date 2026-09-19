-- 스탬프를 17개 광역자치단체 전체로 확장 + image_url 제거 (이슈 #296)
--
-- V34는 9개 도만 대상으로 했다. 서울·부산처럼 방문이 많은 지역이 도감에서
-- 통째로 빠져 있어, 특별시·광역시 8곳을 추가해 17개 시·도를 모두 채운다.
--
-- 코드 변경은 없다. StampService.grantForPlace는 stamp_area_codes를 조회해
-- 판정하므로 행만 넣으면 동작한다(V34에서 의도한 확장 방식).

-- ── 특별시·광역시 8곳 ─────────────────────────────────────────────────
INSERT INTO stamps (stamp_id, stamp_name, created_at, updated_at)
SELECT gen_random_uuid(), v.name, now(), now()
FROM (VALUES
    ('서울'), ('부산'), ('대구'), ('인천'),
    ('광주'), ('대전'), ('울산'), ('세종')
) AS v(name)
WHERE NOT EXISTS (SELECT 1 FROM stamps s WHERE s.stamp_name = v.name);

-- ── areaCode 매핑 ────────────────────────────────────────────────────
-- TourAPI areaCode. 도(31~39)는 V34에서 넣었고 여기서 광역시(1~8)를 채운다.
INSERT INTO stamp_area_codes (area_code, stamp_id)
SELECT v.code, s.stamp_id
FROM (VALUES
    (1, '서울'), (2, '인천'), (3, '대전'), (4, '대구'),
    (5, '광주'), (6, '부산'), (7, '울산'), (8, '세종')
) AS v(code, name)
JOIN stamps s ON s.stamp_name = v.name
ON CONFLICT (area_code) DO NOTHING;

-- ── 이미지 URL 제거 ───────────────────────────────────────────────────
-- 스탬프 이미지는 프론트 저장소에서 관리한다. 서버가 URL을 들고 있을 이유가 없고
-- 실제로 한 번도 채운 적이 없다.
ALTER TABLE stamps DROP COLUMN image_url;
