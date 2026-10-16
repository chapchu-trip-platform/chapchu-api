INSERT INTO regions (region_id, region_name, created_at, updated_at) VALUES
  (gen_random_uuid(), '서울', now(), now()),
  (gen_random_uuid(), '부산', now(), now()),
  (gen_random_uuid(), '제주', now(), now()),
  (gen_random_uuid(), '강원', now(), now());

INSERT INTO themes (theme_id, theme_name, created_at, updated_at) VALUES
  (gen_random_uuid(), '카페', now(), now()),
  (gen_random_uuid(), '공원', now(), now()),
  (gen_random_uuid(), '해변', now(), now()),
  (gen_random_uuid(), '숙박', now(), now());

INSERT INTO transport_methods (transport_method_id, transport_method_name, created_at, updated_at) VALUES
  (gen_random_uuid(), '자가용', now(), now()),
  (gen_random_uuid(), '대중교통', now(), now()),
  (gen_random_uuid(), '도보', now(), now());
