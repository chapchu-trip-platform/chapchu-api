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
