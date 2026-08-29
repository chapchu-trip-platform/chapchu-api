CREATE TABLE review_embeddings (
    review_id  UUID PRIMARY KEY REFERENCES reviews(review_id) ON DELETE CASCADE,
    embedding  vector(1536) NOT NULL,
    created_at TIMESTAMP DEFAULT now()
);

CREATE INDEX idx_review_embeddings_hnsw
    ON review_embeddings USING hnsw (embedding vector_cosine_ops);
