-- 반려견 활동 유형 코드. V1에서 테이블만 만들고 값을 심지 않아 비어 있었다.
-- 엔티티·리포지토리·API도 없어서 pet_preferences_activities를 채울 방법이 전혀 없었다.
--
-- activity_name에 UNIQUE를 걸어 같은 이름이 두 번 들어가지 않게 한다.
-- (ALTER TABLE 자체는 재실행 불가다. Flyway가 마이그레이션을 한 번만 돌리므로 문제되지 않는다.)

ALTER TABLE pet_activities ADD CONSTRAINT uq_pet_activities_activity_name UNIQUE (activity_name);

INSERT INTO pet_activities (activity_id, activity_name, created_at, updated_at) VALUES
  (gen_random_uuid(), '산책', now(), now()),
  (gen_random_uuid(), '뛰놀기', now(), now()),
  (gen_random_uuid(), '공놀이', now(), now()),
  (gen_random_uuid(), '원반놀이', now(), now()),
  (gen_random_uuid(), '수영', now(), now()),
  (gen_random_uuid(), '물놀이', now(), now()),
  (gen_random_uuid(), '등산', now(), now()),
  (gen_random_uuid(), '하이킹', now(), now()),
  (gen_random_uuid(), '캠핑', now(), now()),
  (gen_random_uuid(), '애견운동장', now(), now()),
  (gen_random_uuid(), '카페 나들이', now(), now()),
  (gen_random_uuid(), '드라이브', now(), now()),
  (gen_random_uuid(), '훈련', now(), now()),
  (gen_random_uuid(), '사회화', now(), now()),
  (gen_random_uuid(), '휴식', now(), now())
ON CONFLICT (activity_name) DO NOTHING;
