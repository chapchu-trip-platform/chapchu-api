-- posts.pet_id, posts.course_id NOT NULL 해제 (이슈 #192)
--
-- posts는 자유게시판이다. 리뷰 게시판은 review 도메인에서 따로 가져온다.
-- 자유게시판 글에는 붙일 반려동물도 여행 코스도 없는데 V1 스키마가 둘 다
-- NOT NULL로 잡고 있어서, 없는 코스를 만들어 붙이지 않으면 글을 쓸 수 없었다.
--
-- FK는 그대로 둔다. NULL은 참조 무결성 검사를 건너뛴다.
-- DROP NOT NULL은 카탈로그만 바꾸는 즉시 연산이라 테이블 재작성이 없다.
ALTER TABLE posts
    ALTER COLUMN pet_id DROP NOT NULL;

ALTER TABLE posts
    ALTER COLUMN course_id DROP NOT NULL;

COMMENT ON COLUMN posts.pet_id IS '게시글에 등장하는 반려동물. NULL 가능. FK → pets';
COMMENT ON COLUMN posts.course_id IS '연결된 여행 코스. NULL 가능. FK → travel_courses';
