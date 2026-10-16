ALTER TABLE posts ADD COLUMN comment_count INT DEFAULT 0;

UPDATE posts
SET comment_count = (
    SELECT COUNT(*) FROM comments WHERE comments.post_id = posts.post_id
);
