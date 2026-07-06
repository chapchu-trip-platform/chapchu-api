-- PetTrip PostgreSQL DDL
-- 변경 이력: MySQL → PostgreSQL 전환 + Critical 이슈 수정
--
-- 주요 변경사항:
--   CHAR(36)           → UUID (네이티브 타입)
--   DATETIME           → TIMESTAMP
--   TINYINT            → SMALLINT
--   start_course PK    → UUID (기존 INT에서 통일)
--   keyring_cards.user_id → UUID (기존 VARCHAR(255) 타입 오류 수정)
--   user_preferences   → 3개 junction table로 분리 (다중 선호 지원)
--   pet_preferences_activities PK → (pet_id, activity_id) 복합 PK
--   post_recommendations PK → (post_id, user_id) 복합 PK
--   review_recommendations PK → (review_id, user_id) 복합 PK
--   post_bookmarks PK  → (user_id, post_id) 복합 PK
--   post_reports PK    → (post_id, user_id) 복합 PK
--   comments.post_comment_id → parent_comment_id, NULL 허용 (최상위 댓글 지원)
--   pets.Field         → 삭제 (용도 불명 컬럼)
--   place_pet_policies.indoor_outdoor_type → VARCHAR(20) (BOOLEAN 표현력 부족)
--   course_embeddings  → 신규 (RAG 파이프라인)
--   place_embeddings   → 신규 (장소 기반 RAG)
--   모든 FK 제약조건   → 인라인 선언으로 통일

CREATE EXTENSION IF NOT EXISTS pg_uuidv7;
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- REFERENCE TABLES (도메인 무속, 공유 코드성 테이블)
-- ============================================================

CREATE TABLE themes (
    theme_id         UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    theme_name       VARCHAR(50) NOT NULL,
    created_at       TIMESTAMP DEFAULT now(),
    updated_at       TIMESTAMP DEFAULT now()
);

CREATE TABLE regions (
    region_id        UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    region_name      VARCHAR(100) NOT NULL,
    created_at       TIMESTAMP DEFAULT now(),
    updated_at       TIMESTAMP DEFAULT now()
);

CREATE TABLE transport_methods (
    transport_method_id   UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    transport_method_name VARCHAR(50) NOT NULL,
    created_at            TIMESTAMP DEFAULT now(),
    updated_at            TIMESTAMP DEFAULT now()
);

CREATE TABLE breeds (
    breed_id    UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    breed_name  VARCHAR(30) NOT NULL,
    created_at  TIMESTAMP DEFAULT now(),
    updated_at  TIMESTAMP DEFAULT now()
);

CREATE TABLE pet_activities (
    activity_id   UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    activity_name VARCHAR(30) NOT NULL,
    created_at    TIMESTAMP DEFAULT now(),
    updated_at    TIMESTAMP DEFAULT now()
);

CREATE TABLE stamps (
    stamp_id    UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    stamp_name  VARCHAR(30) NOT NULL,
    created_at  TIMESTAMP DEFAULT now(),
    updated_at  TIMESTAMP DEFAULT now()
);

-- ============================================================
-- USER DOMAIN
-- ============================================================

CREATE TABLE users (
    user_id        UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    google_user_id VARCHAR(255) UNIQUE,
    email          VARCHAR(255) UNIQUE NOT NULL,
    nickname       VARCHAR(30),
    role           VARCHAR(20) DEFAULT 'USER',
    account_status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at     TIMESTAMP DEFAULT now(),
    updated_at     TIMESTAMP DEFAULT now()
);

-- 기존 user_preferences(단일 row) → 3개 junction table로 분리
-- 이유: 사용자가 여러 교통수단·지역을 선호할 수 있음
CREATE TABLE user_preference_transport_methods (
    user_id             UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    transport_method_id UUID NOT NULL REFERENCES transport_methods(transport_method_id),
    created_at          TIMESTAMP DEFAULT now(),
    PRIMARY KEY (user_id, transport_method_id)
);

CREATE TABLE user_preference_regions (
    user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    region_id  UUID NOT NULL REFERENCES regions(region_id),
    created_at TIMESTAMP DEFAULT now(),
    PRIMARY KEY (user_id, region_id)
);

CREATE TABLE user_preference_themes (
    user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    theme_id   UUID NOT NULL REFERENCES themes(theme_id),
    created_at TIMESTAMP DEFAULT now(),
    PRIMARY KEY (user_id, theme_id)
);

CREATE TABLE user_stamps (
    user_stamp_id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_id       UUID REFERENCES users(user_id) ON DELETE SET NULL,
    stamp_id      UUID NOT NULL REFERENCES stamps(stamp_id),
    stamp_count   INT DEFAULT 0,
    created_at    TIMESTAMP DEFAULT now(),
    updated_at    TIMESTAMP DEFAULT now()
);

-- ============================================================
-- PET DOMAIN
-- ============================================================

CREATE TABLE pets (
    pet_id     UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_id    UUID REFERENCES users(user_id) ON DELETE SET NULL,
    breed_id   UUID NOT NULL REFERENCES breeds(breed_id),
    pet_name   VARCHAR(50) NOT NULL,
    size       VARCHAR(10),
    age        SMALLINT,
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE pet_preferences_activities (
    pet_id      UUID NOT NULL REFERENCES pets(pet_id) ON DELETE CASCADE,
    activity_id UUID NOT NULL REFERENCES pet_activities(activity_id),
    created_at  TIMESTAMP DEFAULT now(),
    PRIMARY KEY (pet_id, activity_id)
);

-- ============================================================
-- PLACE DOMAIN
-- ============================================================

CREATE TABLE places (
    external_place_id VARCHAR(255) PRIMARY KEY,
    theme_id          UUID REFERENCES themes(theme_id),
    place_name        VARCHAR(100),
    place_image_url   VARCHAR(500),
    address           VARCHAR(255),
    latitude          DECIMAL(10,7),
    longitude         DECIMAL(10,7),
    business_hours    VARCHAR(255),
    phone_number      VARCHAR(30),
    rating            SMALLINT,
    review_num        INT DEFAULT 0,
    visit_num         INT DEFAULT 0,
    created_at        TIMESTAMP DEFAULT now(),
    updated_at        TIMESTAMP DEFAULT now()
);

CREATE TABLE place_pet_policies (
    external_place_id VARCHAR(255) PRIMARY KEY REFERENCES places(external_place_id),
    allowed_pet_size  VARCHAR(10),
    leash_required    BOOLEAN,
    carrier_required  BOOLEAN,
    indoor_outdoor_type VARCHAR(20),   -- 'INDOOR' | 'OUTDOOR' | 'BOTH'
    parking           BOOLEAN,
    place_caution     TEXT,
    created_at        TIMESTAMP DEFAULT now(),
    updated_at        TIMESTAMP DEFAULT now()
);

CREATE TABLE place_embeddings (
    external_place_id VARCHAR(255) PRIMARY KEY REFERENCES places(external_place_id),
    embedding         vector(3072) NOT NULL,
    created_at        TIMESTAMP DEFAULT now()
);

-- ============================================================
-- TRIP DOMAIN
-- ============================================================

CREATE TABLE start_course (
    start_course_id       UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    start_course_location VARCHAR(255),
    start_course_time     TIMESTAMP
);

CREATE TABLE travel_courses (
    course_id       UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_id         UUID REFERENCES users(user_id) ON DELETE SET NULL,
    start_course_id UUID NOT NULL REFERENCES start_course(start_course_id),
    travel_date     DATE,
    is_completed    BOOLEAN DEFAULT false,
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE TABLE course_places (
    course_place_id  UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    course_id        UUID NOT NULL REFERENCES travel_courses(course_id) ON DELETE CASCADE,
    external_place_id VARCHAR(255) NOT NULL REFERENCES places(external_place_id),
    visit_order      SMALLINT,
    final_place      BOOLEAN DEFAULT false,
    is_visited       BOOLEAN DEFAULT false,
    visited_at       TIMESTAMP,
    created_at       TIMESTAMP DEFAULT now(),
    updated_at       TIMESTAMP DEFAULT now()
);

CREATE TABLE course_routes (
    route_id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    course_id         UUID NOT NULL REFERENCES travel_courses(course_id) ON DELETE CASCADE,
    transport_method  VARCHAR(20),
    total_distance_m  INT,
    total_duration_sec INT,
    created_at        TIMESTAMP DEFAULT now(),
    updated_at        TIMESTAMP DEFAULT now()
);

CREATE TABLE course_route_legs (
    route_leg_id        UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    route_id            UUID NOT NULL REFERENCES course_routes(route_id) ON DELETE CASCADE,
    from_course_place_id UUID NOT NULL REFERENCES course_places(course_place_id),
    leg_order           SMALLINT,
    distance_m          INT,
    duration_sec        INT,
    leg_polyline        TEXT,
    instruction         TEXT,
    created_at          TIMESTAMP DEFAULT now(),
    updated_at          TIMESTAMP DEFAULT now()
);

CREATE TABLE course_weather_records (
    weather_id      UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    course_id       UUID NOT NULL REFERENCES travel_courses(course_id) ON DELETE CASCADE,
    weather_date    DATE,
    temperature     SMALLINT,
    humidity        SMALLINT,
    weather_status  VARCHAR(10),
    weather_caution VARCHAR(30),
    created_at      TIMESTAMP DEFAULT now(),
    updated_at      TIMESTAMP DEFAULT now()
);

CREATE TABLE photos (
    photo_id        UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_id         UUID REFERENCES users(user_id) ON DELETE SET NULL,
    course_place_id UUID NOT NULL REFERENCES course_places(course_place_id),
    photo_url       VARCHAR(500),
    taken_at        DATE,
    created_at      TIMESTAMP DEFAULT now()
);

CREATE TABLE visit_verifications (
    verification_id       UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_id               UUID REFERENCES users(user_id) ON DELETE SET NULL,
    course_place_id       UUID NOT NULL REFERENCES course_places(course_place_id),
    photo_id              UUID NOT NULL REFERENCES photos(photo_id),
    verification_latitude  DECIMAL(10,7),
    verification_longitude DECIMAL(10,7),
    verification_status   VARCHAR(30),
    verified_at           TIMESTAMP,
    created_at            TIMESTAMP DEFAULT now(),
    updated_at            TIMESTAMP DEFAULT now()
);

CREATE TABLE course_embeddings (
    course_id  UUID PRIMARY KEY REFERENCES travel_courses(course_id) ON DELETE CASCADE,
    embedding  vector(3072) NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

-- ============================================================
-- REVIEW DOMAIN
-- ============================================================

CREATE TABLE reviews (
    review_id            UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    place_id             VARCHAR(255) NOT NULL REFERENCES places(external_place_id),
    user_id              UUID REFERENCES users(user_id) ON DELETE SET NULL,
    pet_id               UUID NOT NULL REFERENCES pets(pet_id),
    rating               SMALLINT,
    contents             TEXT,
    recommendation_count INT DEFAULT 0,
    created_at           TIMESTAMP DEFAULT now()
);

CREATE TABLE review_recommendations (
    review_id  UUID NOT NULL REFERENCES reviews(review_id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT now(),
    PRIMARY KEY (review_id, user_id)
);

-- ============================================================
-- COMMUNITY DOMAIN
-- ============================================================

CREATE TABLE posts (
    post_id              UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_id              UUID REFERENCES users(user_id) ON DELETE SET NULL,
    pet_id               UUID NOT NULL REFERENCES pets(pet_id),
    photo_id             UUID NOT NULL REFERENCES photos(photo_id),
    course_id            UUID NOT NULL REFERENCES travel_courses(course_id),
    title                VARCHAR(100),
    content              TEXT,
    view_count           INT DEFAULT 0,
    recommendation_count INT DEFAULT 0,
    created_at           TIMESTAMP DEFAULT now()
);

CREATE TABLE comments (
    comment_id        UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    post_id           UUID NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    user_id           UUID REFERENCES users(user_id) ON DELETE SET NULL,
    parent_comment_id UUID REFERENCES comments(comment_id) ON DELETE CASCADE,  -- NULL = 최상위 댓글
    depth             INT DEFAULT 0,
    comment_order     INT,
    content           TEXT,
    created_at        TIMESTAMP DEFAULT now()
);

CREATE TABLE post_recommendations (
    post_id    UUID NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE TABLE post_bookmarks (
    user_id    UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    post_id    UUID NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT now(),
    PRIMARY KEY (user_id, post_id)
);

CREATE TABLE post_reports (
    post_id       UUID NOT NULL REFERENCES posts(post_id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    report_reason VARCHAR(50),
    report_detail TEXT,
    report_status VARCHAR(20) DEFAULT 'PENDING',
    created_at    TIMESTAMP DEFAULT now(),
    updated_at    TIMESTAMP DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

-- ============================================================
-- ALBUM DOMAIN
-- ============================================================

CREATE TABLE albums (
    album_id   UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_id    UUID REFERENCES users(user_id) ON DELETE SET NULL,
    album_name VARCHAR(30),
    created_at TIMESTAMP DEFAULT now(),
    updated_at TIMESTAMP DEFAULT now()
);

CREATE TABLE album_photos (
    album_photo_id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    album_id       UUID NOT NULL REFERENCES albums(album_id) ON DELETE CASCADE,
    photo_id       UUID NOT NULL REFERENCES photos(photo_id) ON DELETE CASCADE,
    created_at     TIMESTAMP DEFAULT now(),
    updated_at     TIMESTAMP DEFAULT now()
);

CREATE TABLE keyring_cards (
    keyring_card_id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_id         UUID REFERENCES users(user_id) ON DELETE SET NULL,
    pet_id          UUID NOT NULL REFERENCES pets(pet_id),
    photo_id        UUID NOT NULL REFERENCES photos(photo_id),
    course_id       UUID NOT NULL REFERENCES travel_courses(course_id),
    keyring_card_url VARCHAR(500),
    created_at      TIMESTAMP DEFAULT now()
);

-- ============================================================
-- INDEXES
-- ============================================================

CREATE INDEX idx_travel_courses_user_id ON travel_courses(user_id);
CREATE INDEX idx_course_places_course_id ON course_places(course_id);
CREATE INDEX idx_reviews_place_id ON reviews(place_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_posts_user_id ON posts(user_id);
CREATE INDEX idx_comments_post_id ON comments(post_id);
CREATE INDEX idx_pets_user_id ON pets(user_id);
CREATE INDEX idx_photos_course_place_id ON photos(course_place_id);

-- 벡터 ANN 탐색용 HNSW 인덱스 (코사인 유사도)
CREATE INDEX idx_course_embeddings_hnsw ON course_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_place_embeddings_hnsw ON place_embeddings USING hnsw (embedding vector_cosine_ops);

-- ============================================================
-- COMMENTS
-- ============================================================

-- REFERENCE TABLES
COMMENT ON TABLE themes IS '여행 테마 코드 테이블 (예: 카페, 공원, 해변)';
COMMENT ON COLUMN themes.theme_id IS '기본키';
COMMENT ON COLUMN themes.theme_name IS '테마 이름';

COMMENT ON TABLE regions IS '지역 코드 테이블 (예: 서울, 부산, 제주)';
COMMENT ON COLUMN regions.region_id IS '기본키';
COMMENT ON COLUMN regions.region_name IS '지역 이름';

COMMENT ON TABLE transport_methods IS '이동 수단 코드 테이블 (예: 자가용, 대중교통, 도보)';
COMMENT ON COLUMN transport_methods.transport_method_id IS '기본키';
COMMENT ON COLUMN transport_methods.transport_method_name IS '이동 수단 이름';

COMMENT ON TABLE breeds IS '견종 코드 테이블';
COMMENT ON COLUMN breeds.breed_id IS '기본키';
COMMENT ON COLUMN breeds.breed_name IS '견종 이름';

COMMENT ON TABLE pet_activities IS '반려견 활동 유형 코드 테이블 (예: 산책, 수영, 하이킹)';
COMMENT ON COLUMN pet_activities.activity_id IS '기본키';
COMMENT ON COLUMN pet_activities.activity_name IS '활동 이름';

COMMENT ON TABLE stamps IS '스탬프 종류 코드 테이블. 지역별 방문 달성 보상';
COMMENT ON COLUMN stamps.stamp_id IS '기본키';
COMMENT ON COLUMN stamps.stamp_name IS '스탬프 이름';

-- USER DOMAIN
COMMENT ON TABLE users IS '서비스 회원. Google OAuth 2.0으로 가입';
COMMENT ON COLUMN users.user_id IS '기본키. UUID v7';
COMMENT ON COLUMN users.google_user_id IS 'Google OAuth sub 값. 소셜 로그인 식별자';
COMMENT ON COLUMN users.email IS '이메일. UNIQUE';
COMMENT ON COLUMN users.nickname IS '서비스 내 표시 이름';
COMMENT ON COLUMN users.role IS '권한. ENUM(USER, ADMIN)';
COMMENT ON COLUMN users.account_status IS '계정 상태. ENUM(ACTIVE, WITHDRAWN)';

COMMENT ON TABLE user_preference_transport_methods IS '사용자 선호 이동 수단. N:M junction table';
COMMENT ON COLUMN user_preference_transport_methods.user_id IS 'FK → users';
COMMENT ON COLUMN user_preference_transport_methods.transport_method_id IS 'FK → transport_methods';

COMMENT ON TABLE user_preference_regions IS '사용자 선호 지역. N:M junction table';
COMMENT ON COLUMN user_preference_regions.user_id IS 'FK → users';
COMMENT ON COLUMN user_preference_regions.region_id IS 'FK → regions';

COMMENT ON TABLE user_preference_themes IS '사용자 선호 테마. N:M junction table';
COMMENT ON COLUMN user_preference_themes.user_id IS 'FK → users';
COMMENT ON COLUMN user_preference_themes.theme_id IS 'FK → themes';

COMMENT ON TABLE user_stamps IS '사용자별 스탬프 획득 현황';
COMMENT ON COLUMN user_stamps.user_stamp_id IS '기본키';
COMMENT ON COLUMN user_stamps.user_id IS 'FK → users. 탈퇴 시 NULL 유지 (이력 보존)';
COMMENT ON COLUMN user_stamps.stamp_id IS 'FK → stamps';
COMMENT ON COLUMN user_stamps.stamp_count IS '해당 스탬프 획득 횟수';

-- PET DOMAIN
COMMENT ON TABLE pets IS '반려동물 정보. 사용자당 복수 등록 가능';
COMMENT ON COLUMN pets.pet_id IS '기본키. UUID v7';
COMMENT ON COLUMN pets.user_id IS 'FK → users. 탈퇴 시 NULL 유지';
COMMENT ON COLUMN pets.breed_id IS 'FK → breeds';
COMMENT ON COLUMN pets.pet_name IS '반려동물 이름';
COMMENT ON COLUMN pets.size IS '체형. ENUM(SMALL, MEDIUM, LARGE)';
COMMENT ON COLUMN pets.age IS '나이 (년)';

COMMENT ON TABLE pet_preferences_activities IS '반려동물 선호 활동. N:M junction table';
COMMENT ON COLUMN pet_preferences_activities.pet_id IS 'FK → pets';
COMMENT ON COLUMN pet_preferences_activities.activity_id IS 'FK → pet_activities';

-- PLACE DOMAIN
COMMENT ON TABLE places IS '반려동물 동반 가능 장소. 외부 API(카카오 등) 기준 식별자 사용';
COMMENT ON COLUMN places.external_place_id IS '외부 API 장소 식별자. 기본키';
COMMENT ON COLUMN places.theme_id IS 'FK → themes';
COMMENT ON COLUMN places.place_name IS '장소명';
COMMENT ON COLUMN places.place_image_url IS '대표 이미지 URL';
COMMENT ON COLUMN places.address IS '주소';
COMMENT ON COLUMN places.latitude IS '위도';
COMMENT ON COLUMN places.longitude IS '경도';
COMMENT ON COLUMN places.business_hours IS '영업시간';
COMMENT ON COLUMN places.phone_number IS '전화번호';
COMMENT ON COLUMN places.rating IS '평균 평점';
COMMENT ON COLUMN places.review_num IS '리뷰 수. 비정규화 카운터';
COMMENT ON COLUMN places.visit_num IS '방문 인증 수. 비정규화 카운터';

COMMENT ON TABLE place_pet_policies IS '장소별 반려동물 입장 정책. places와 1:1';
COMMENT ON COLUMN place_pet_policies.external_place_id IS 'PK이자 FK → places';
COMMENT ON COLUMN place_pet_policies.allowed_pet_size IS '입장 가능 체형. ENUM(SMALL, MEDIUM, LARGE, ALL)';
COMMENT ON COLUMN place_pet_policies.leash_required IS '목줄 필수 여부';
COMMENT ON COLUMN place_pet_policies.carrier_required IS '케이지 필수 여부';
COMMENT ON COLUMN place_pet_policies.indoor_outdoor_type IS '실내외 구분. ENUM(INDOOR, OUTDOOR, BOTH)';
COMMENT ON COLUMN place_pet_policies.parking IS '주차 가능 여부';
COMMENT ON COLUMN place_pet_policies.place_caution IS '기타 주의사항';

COMMENT ON TABLE place_embeddings IS '장소 텍스트 임베딩. RAG 장소 유사도 검색용. append-only';
COMMENT ON COLUMN place_embeddings.external_place_id IS 'PK이자 FK → places';
COMMENT ON COLUMN place_embeddings.embedding IS 'OpenAI text-embedding-3-large 벡터. 차원 3072';

-- TRIP DOMAIN
COMMENT ON TABLE start_course IS '여행 코스 출발지 정보';
COMMENT ON COLUMN start_course.start_course_id IS '기본키. UUID v7';
COMMENT ON COLUMN start_course.start_course_location IS '출발지 주소 또는 장소명';
COMMENT ON COLUMN start_course.start_course_time IS '출발 시각';

COMMENT ON TABLE travel_courses IS '사용자가 생성한 반려동물 동반 여행 코스';
COMMENT ON COLUMN travel_courses.course_id IS '기본키. UUID v7';
COMMENT ON COLUMN travel_courses.user_id IS 'FK → users. 탈퇴 시 NULL 유지';
COMMENT ON COLUMN travel_courses.start_course_id IS 'FK → start_course';
COMMENT ON COLUMN travel_courses.travel_date IS '여행 날짜';
COMMENT ON COLUMN travel_courses.is_completed IS '코스 완료 여부. 마지막 장소 방문 인증 시 true';

COMMENT ON TABLE course_places IS '코스에 포함된 방문 장소 목록. 순서 정보 포함';
COMMENT ON COLUMN course_places.course_place_id IS '기본키. UUID v7';
COMMENT ON COLUMN course_places.course_id IS 'FK → travel_courses';
COMMENT ON COLUMN course_places.external_place_id IS 'FK → places';
COMMENT ON COLUMN course_places.visit_order IS '방문 순서 (1부터 시작)';
COMMENT ON COLUMN course_places.final_place IS '마지막 방문지 여부';
COMMENT ON COLUMN course_places.is_visited IS '방문 인증 완료 여부';
COMMENT ON COLUMN course_places.visited_at IS '방문 인증 시각';

COMMENT ON TABLE course_routes IS '코스 전체 경로 요약 정보';
COMMENT ON COLUMN course_routes.route_id IS '기본키. UUID v7';
COMMENT ON COLUMN course_routes.course_id IS 'FK → travel_courses';
COMMENT ON COLUMN course_routes.transport_method IS '이동 수단';
COMMENT ON COLUMN course_routes.total_distance_m IS '총 이동 거리 (미터)';
COMMENT ON COLUMN course_routes.total_duration_sec IS '총 소요 시간 (초)';

COMMENT ON TABLE course_route_legs IS '코스 구간별 상세 경로. 장소 A → 장소 B 단위';
COMMENT ON COLUMN course_route_legs.route_leg_id IS '기본키. UUID v7';
COMMENT ON COLUMN course_route_legs.route_id IS 'FK → course_routes';
COMMENT ON COLUMN course_route_legs.from_course_place_id IS '출발 장소. FK → course_places';
COMMENT ON COLUMN course_route_legs.leg_order IS '구간 순서';
COMMENT ON COLUMN course_route_legs.distance_m IS '구간 거리 (미터)';
COMMENT ON COLUMN course_route_legs.duration_sec IS '구간 소요 시간 (초)';
COMMENT ON COLUMN course_route_legs.leg_polyline IS '구간 경로 폴리라인 인코딩값';
COMMENT ON COLUMN course_route_legs.instruction IS '경로 안내 텍스트';

COMMENT ON TABLE course_weather_records IS '여행 당일 날씨 기록. RAG 추천 컨텍스트로 활용';
COMMENT ON COLUMN course_weather_records.weather_id IS '기본키. UUID v7';
COMMENT ON COLUMN course_weather_records.course_id IS 'FK → travel_courses';
COMMENT ON COLUMN course_weather_records.weather_date IS '날씨 기록 날짜';
COMMENT ON COLUMN course_weather_records.temperature IS '기온 (°C)';
COMMENT ON COLUMN course_weather_records.humidity IS '습도 (%)';
COMMENT ON COLUMN course_weather_records.weather_status IS '날씨 상태 (예: SUNNY, RAIN, CLOUD)';
COMMENT ON COLUMN course_weather_records.weather_caution IS '날씨 주의사항 (예: 자외선 강함)';

COMMENT ON TABLE photos IS '방문 인증 및 게시글에 사용된 사진. S3 저장 후 URL만 보관';
COMMENT ON COLUMN photos.photo_id IS '기본키. UUID v7';
COMMENT ON COLUMN photos.user_id IS 'FK → users. 탈퇴 시 NULL 유지';
COMMENT ON COLUMN photos.course_place_id IS '촬영한 코스 장소. FK → course_places';
COMMENT ON COLUMN photos.photo_url IS 'S3 저장 경로. EXIF 제거 후 업로드';
COMMENT ON COLUMN photos.taken_at IS '촬영 날짜';

COMMENT ON TABLE visit_verifications IS '장소 방문 인증 기록. GPS + 사진으로 검증';
COMMENT ON COLUMN visit_verifications.verification_id IS '기본키. UUID v7';
COMMENT ON COLUMN visit_verifications.user_id IS 'FK → users. 탈퇴 시 NULL 유지';
COMMENT ON COLUMN visit_verifications.course_place_id IS 'FK → course_places';
COMMENT ON COLUMN visit_verifications.photo_id IS '인증 사진. FK → photos';
COMMENT ON COLUMN visit_verifications.verification_latitude IS '인증 시점 GPS 위도';
COMMENT ON COLUMN visit_verifications.verification_longitude IS '인증 시점 GPS 경도';
COMMENT ON COLUMN visit_verifications.verification_status IS '인증 상태. ENUM(PENDING, APPROVED, REJECTED)';
COMMENT ON COLUMN visit_verifications.verified_at IS '인증 처리 시각';

COMMENT ON TABLE course_embeddings IS '여행 코스 임베딩. RAG 유사 코스 추천용. append-only';
COMMENT ON COLUMN course_embeddings.course_id IS 'PK이자 FK → travel_courses';
COMMENT ON COLUMN course_embeddings.embedding IS 'OpenAI text-embedding-3-large 벡터. 차원 3072';

-- REVIEW DOMAIN
COMMENT ON TABLE reviews IS '장소 방문 후 작성하는 리뷰. 반려동물 동반 경험 중심';
COMMENT ON COLUMN reviews.review_id IS '기본키. UUID v7';
COMMENT ON COLUMN reviews.place_id IS 'FK → places';
COMMENT ON COLUMN reviews.user_id IS 'FK → users. 탈퇴 시 NULL 유지';
COMMENT ON COLUMN reviews.pet_id IS '리뷰 작성 시 동반한 반려동물. FK → pets';
COMMENT ON COLUMN reviews.rating IS '별점 (1~5)';
COMMENT ON COLUMN reviews.contents IS '리뷰 본문';
COMMENT ON COLUMN reviews.recommendation_count IS '추천 수. 비정규화 카운터';

COMMENT ON TABLE review_recommendations IS '리뷰 추천. N:M junction table. append-only';
COMMENT ON COLUMN review_recommendations.review_id IS 'FK → reviews';
COMMENT ON COLUMN review_recommendations.user_id IS 'FK → users';

-- COMMUNITY DOMAIN
COMMENT ON TABLE posts IS '커뮤니티 게시글. 여행 코스 후기 공유';
COMMENT ON COLUMN posts.post_id IS '기본키. UUID v7';
COMMENT ON COLUMN posts.user_id IS 'FK → users. 탈퇴 시 NULL 유지';
COMMENT ON COLUMN posts.pet_id IS '게시글에 등장하는 반려동물. FK → pets';
COMMENT ON COLUMN posts.photo_id IS '대표 사진. FK → photos';
COMMENT ON COLUMN posts.course_id IS '연결된 여행 코스. FK → travel_courses';
COMMENT ON COLUMN posts.title IS '게시글 제목';
COMMENT ON COLUMN posts.content IS '게시글 본문';
COMMENT ON COLUMN posts.view_count IS '조회수. 비정규화 카운터';
COMMENT ON COLUMN posts.recommendation_count IS '추천 수. 비정규화 카운터';

COMMENT ON TABLE comments IS '게시글 댓글. parent_comment_id NULL이면 최상위 댓글';
COMMENT ON COLUMN comments.comment_id IS '기본키. UUID v7';
COMMENT ON COLUMN comments.post_id IS 'FK → posts';
COMMENT ON COLUMN comments.user_id IS 'FK → users. 탈퇴 시 NULL 유지';
COMMENT ON COLUMN comments.parent_comment_id IS '부모 댓글. NULL = 최상위 댓글. FK → comments (자기참조)';
COMMENT ON COLUMN comments.depth IS '댓글 깊이. 최상위=0, 대댓글=1';
COMMENT ON COLUMN comments.comment_order IS '같은 깊이 내 정렬 순서';
COMMENT ON COLUMN comments.content IS '댓글 본문';

COMMENT ON TABLE post_recommendations IS '게시글 추천. N:M junction table. append-only';
COMMENT ON COLUMN post_recommendations.post_id IS 'FK → posts';
COMMENT ON COLUMN post_recommendations.user_id IS 'FK → users';

COMMENT ON TABLE post_bookmarks IS '게시글 북마크. N:M junction table';
COMMENT ON COLUMN post_bookmarks.user_id IS 'FK → users';
COMMENT ON COLUMN post_bookmarks.post_id IS 'FK → posts';

COMMENT ON TABLE post_reports IS '게시글 신고. N:M junction table';
COMMENT ON COLUMN post_reports.post_id IS 'FK → posts';
COMMENT ON COLUMN post_reports.user_id IS '신고한 사용자. FK → users';
COMMENT ON COLUMN post_reports.report_reason IS '신고 사유 분류';
COMMENT ON COLUMN post_reports.report_detail IS '신고 상세 내용';
COMMENT ON COLUMN post_reports.report_status IS '처리 상태. ENUM(PENDING, RESOLVED, DISMISSED)';

-- ALBUM DOMAIN
COMMENT ON TABLE albums IS '사용자가 직접 만드는 사진 앨범';
COMMENT ON COLUMN albums.album_id IS '기본키. UUID v7';
COMMENT ON COLUMN albums.user_id IS 'FK → users. 탈퇴 시 NULL 유지';
COMMENT ON COLUMN albums.album_name IS '앨범 이름';

COMMENT ON TABLE album_photos IS '앨범-사진 N:M junction table';
COMMENT ON COLUMN album_photos.album_photo_id IS '기본키. UUID v7';
COMMENT ON COLUMN album_photos.album_id IS 'FK → albums';
COMMENT ON COLUMN album_photos.photo_id IS 'FK → photos';

COMMENT ON TABLE keyring_cards IS '여행 완료 기념 카드. 코스 완료 시 자동 생성';
COMMENT ON COLUMN keyring_cards.keyring_card_id IS '기본키. UUID v7';
COMMENT ON COLUMN keyring_cards.user_id IS 'FK → users. 탈퇴 시 NULL 유지';
COMMENT ON COLUMN keyring_cards.pet_id IS '동반 반려동물. FK → pets';
COMMENT ON COLUMN keyring_cards.photo_id IS '대표 사진. FK → photos';
COMMENT ON COLUMN keyring_cards.course_id IS '완료한 여행 코스. FK → travel_courses';
COMMENT ON COLUMN keyring_cards.keyring_card_url IS '생성된 카드 이미지 S3 URL';
