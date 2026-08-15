# 036. breeds 테이블 PK를 INT IDENTITY로 — UUID PK 정책의 예외

## 배경

프로젝트 전체 PK 정책(005)은 UUID v7(`uuid_generate_v7()`)이다.
그러나 `breeds`는 고정 코드값 테이블로, 운영 팀이 품종을 시딩하고 클라이언트가 그 ID를 API로 받아
그대로 사용한다. UUID PK를 쓰면 시딩 순서마다 값이 달라지고, 환경마다 ID가 다른 문제가 있었다.

초기에는 `breeds.breed_id`가 UUID였다. `GET /breeds`가 UUID 목록을 내보내면 FE가 그걸 기억해
`POST /pets`에 `breedId`로 넘기는 구조라 UUID 자체가 문제는 아니었다.
그러나 UUID로는 시딩 SQL에서 `VALUES (uuid_generate_v7(), '믹스견')` 형태가 돼
환경마다 UUID가 달라져 고정 테스트 케이스를 만들기 어려웠다.

또한 `pg_uuidv7` 확장(failures/015)이 없는 환경에서는 `gen_random_uuid()`로 대체해야 했고
이는 005 결정 취지(B-Tree 인덱스 스플릿 방지)에 어긋난다.

## 결정

`breeds.breed_id`를 `INT GENERATED ALWAYS AS IDENTITY PRIMARY KEY`로 전환한다.

품종 코드값은 불변이고 순서 의존성이 없어 AUTO INCREMENT INT가 적합하다.
UUID의 단조 증가 보장 요건(인덱스 스플릿 방지)은 코드값 테이블에서는 의미 없다.
삽입 빈도가 극히 낮고(팀이 수동으로 시딩), 분산 생성도 필요 없다.

`V7__breed_id_to_int.sql`로 마이그레이션: pets + breeds 기존 데이터 삭제 후 테이블 재생성.
`V8__seed_breeds_full.sql`로 157종 재시딩.

### BaseEntity 상속 예외

`Breed` 엔티티는 BaseEntity를 상속하지 않는다.
BaseEntity는 UUID v7 PK를 전제로 설계됐고, INT PK와 혼용하면 `ddl-auto: validate`가 실패한다.
`Breed`는 직접 `@Id @Column(name = "breed_id") Integer breedId`를 선언한다.

## 에이전트 행동 지침

- `breeds` 테이블의 PK는 INT다. UUID 관련 코드를 추가하지 마라.
- `Breed` 엔티티는 BaseEntity를 상속하지 않는 **공식 예외 케이스**다. 리팩토링 명목으로 바꾸지 마라.
- 향후 유사한 코드값 테이블(stamps, pet_activities 등)이 생기면 같은 패턴을 적용할 수 있다.
- 단, 유저 생성 데이터(pets, users, posts 등)는 반드시 BaseEntity + UUID v7 정책을 유지하라.
