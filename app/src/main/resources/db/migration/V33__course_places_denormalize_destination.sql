-- 도착지를 임의 사용자 선택 고정 끝점으로 변경(decisions/045).
-- 도착지는 places/RAG에 안 쌓지만 course_place 스탑으로는 유지(리뷰·사진·앨범 대상)해야 하므로,
-- external_place_id를 NULL 허용으로 바꾸고 도착지의 이름·좌표를 비정규화 저장한다.
-- 중간 스탑: external_place_id 세팅(신규 컬럼 NULL). 도착지: external_place_id NULL(신규 컬럼 세팅).
ALTER TABLE course_places ALTER COLUMN external_place_id DROP NOT NULL;
ALTER TABLE course_places ADD COLUMN place_name VARCHAR(255);
ALTER TABLE course_places ADD COLUMN latitude DECIMAL(10,7);
ALTER TABLE course_places ADD COLUMN longitude DECIMAL(10,7);

-- 반려견 가능 여부(표시용). 중간 스탑은 크기 필터 통과라 true, 도착지는 요청 정책으로 계산(모르면 null).
-- 막지 않고(선택 보장) 표시만 한다. places/RAG와 무관 — 요청값으로 계산한 결과만 저장.
ALTER TABLE course_places ADD COLUMN pet_allowed BOOLEAN;
