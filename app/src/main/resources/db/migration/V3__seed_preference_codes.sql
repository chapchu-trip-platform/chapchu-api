INSERT INTO regions (region_id, region_name, created_at, updated_at) VALUES
  (uuid_generate_v7(), '서울', now(), now()),
  (uuid_generate_v7(), '부산', now(), now()),
  (uuid_generate_v7(), '제주', now(), now()),
  (uuid_generate_v7(), '강원', now(), now());

INSERT INTO themes (theme_id, theme_name, created_at, updated_at) VALUES
  (uuid_generate_v7(), '카페', now(), now()),
  (uuid_generate_v7(), '공원', now(), now()),
  (uuid_generate_v7(), '해변', now(), now()),
  (uuid_generate_v7(), '숙박', now(), now());

INSERT INTO transport_methods (transport_method_id, transport_method_name, created_at, updated_at) VALUES
  (uuid_generate_v7(), '자가용', now(), now()),
  (uuid_generate_v7(), '대중교통', now(), now()),
  (uuid_generate_v7(), '도보', now(), now());
