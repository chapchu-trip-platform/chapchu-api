-- 코스↔펫 연결. 코스 생성 시 petId를 받지만 저장하지 않아 사진→코스→펫 역추적이 불가능했다.
-- 앨범(코스/펫 단위)과 사후 RAG(펫 크기·나이 재조회)를 위해 pet_id를 저장한다.
ALTER TABLE travel_courses ADD COLUMN pet_id UUID REFERENCES pets(pet_id);

-- 기존 코스 백필: 해당 코스의 스탑에 달린 리뷰의 pet_id로 채운다(리뷰가 없으면 NULL 유지).
UPDATE travel_courses tc
SET pet_id = (
    SELECT r.pet_id
    FROM course_places cp
    JOIN reviews r ON r.course_place_id = cp.course_place_id
    WHERE cp.course_id = tc.course_id
    LIMIT 1
)
WHERE tc.pet_id IS NULL;
