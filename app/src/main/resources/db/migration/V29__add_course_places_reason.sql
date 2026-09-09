-- 코스 스탑별 "왜 이 곳"(AI 큐레이션 이유). 없을 수 있어 NULL 허용.
ALTER TABLE course_places ADD COLUMN reason TEXT;
