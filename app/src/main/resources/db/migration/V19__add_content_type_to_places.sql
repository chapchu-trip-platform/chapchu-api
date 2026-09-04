ALTER TABLE places ADD COLUMN content_type_id SMALLINT;
CREATE INDEX idx_places_content_type ON places(content_type_id);
