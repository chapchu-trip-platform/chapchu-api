-- breeds.breed_id UUID → INT GENERATED ALWAYS AS IDENTITY
-- pets.breed_id UUID → INT
-- UUID-INT 매핑 불가로 기존 pets 데이터 삭제

ALTER TABLE pets DROP CONSTRAINT IF EXISTS pets_breed_id_fkey;
DELETE FROM pets;
ALTER TABLE pets DROP COLUMN breed_id;

DROP TABLE breeds;

CREATE TABLE breeds (
    breed_id    INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    breed_name  VARCHAR(30) NOT NULL,
    created_at  TIMESTAMP DEFAULT now(),
    updated_at  TIMESTAMP DEFAULT now()
);

ALTER TABLE pets ADD COLUMN breed_id INT NOT NULL DEFAULT 1 REFERENCES breeds(breed_id);
ALTER TABLE pets ALTER COLUMN breed_id DROP DEFAULT;
