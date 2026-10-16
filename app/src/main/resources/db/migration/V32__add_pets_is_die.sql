-- 반려동물 사망 여부. 펫 사망 시 펫별 앨범을 추모용으로 노출하기 위한 플래그(decisions/044 연계).
ALTER TABLE pets ADD COLUMN is_die BOOLEAN NOT NULL DEFAULT false;
