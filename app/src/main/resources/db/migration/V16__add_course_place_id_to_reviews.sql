ALTER TABLE reviews
    ADD COLUMN course_place_id UUID NULL
        REFERENCES course_places(course_place_id);

CREATE INDEX idx_reviews_course_place_id
    ON reviews(course_place_id)
    WHERE course_place_id IS NOT NULL;

COMMENT ON COLUMN reviews.course_place_id IS '리뷰가 작성된 코스 방문 장소. NULL이면 코스 외 단독 리뷰';
