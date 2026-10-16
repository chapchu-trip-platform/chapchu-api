-- 코스 도메인 재설계: start_course/route 테이블 제거, 출발지·도착지 컬럼 인라인화

-- 1. travel_courses.start_course_id FK 제약 제거 후 컬럼 삭제
ALTER TABLE travel_courses
    DROP CONSTRAINT IF EXISTS travel_courses_start_course_id_fkey;
ALTER TABLE travel_courses
    DROP COLUMN IF EXISTS start_course_id;

-- 2. travel_courses 출발지·도착지 컬럼 추가
ALTER TABLE travel_courses
    ADD COLUMN start_location   VARCHAR(255),
    ADD COLUMN start_lat        DECIMAL(10, 7),
    ADD COLUMN start_lng        DECIMAL(10, 7),
    ADD COLUMN end_location     VARCHAR(255),
    ADD COLUMN end_lat          DECIMAL(10, 7),
    ADD COLUMN end_lng          DECIMAL(10, 7),
    ADD COLUMN total_distance_m INT;

COMMENT ON COLUMN travel_courses.start_location IS '출발지 주소 또는 장소명';
COMMENT ON COLUMN travel_courses.start_lat IS '출발지 위도';
COMMENT ON COLUMN travel_courses.start_lng IS '출발지 경도';
COMMENT ON COLUMN travel_courses.end_location IS '도착지 주소 또는 장소명';
COMMENT ON COLUMN travel_courses.end_lat IS '도착지 위도';
COMMENT ON COLUMN travel_courses.end_lng IS '도착지 경도';
COMMENT ON COLUMN travel_courses.total_distance_m IS '총 이동 거리 (미터). FE 계산값';

-- 3. course_route_legs 삭제 (course_routes 참조 → 먼저 삭제)
DROP TABLE IF EXISTS course_route_legs;

-- 4. course_routes 삭제
DROP TABLE IF EXISTS course_routes;

-- 5. start_course 삭제 (travel_courses FK 제거 후이므로 안전)
DROP TABLE IF EXISTS start_course;
