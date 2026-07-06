# PostgreSQL에서 UUID v7을 PK로 선택한 이유

PetTrip 백엔드를 설계하면서 PK 타입 하나 때문에 꽤 오래 고민했다.
처음엔 그냥 MySQL 하던 대로 `CHAR(36)`에 UUID 때려 박으면 되지 않나 싶었는데,
파고들수록 생각보다 고려할 게 많았다.

---

## 처음엔 이렇게 짰다

```sql
user_id CHAR(36) NOT NULL
```

MySQL에서 UUID를 쓸 때 늘 이렇게 했다. UUID는 128비트 데이터인데,
MySQL엔 UUID 전용 타입이 없으니까 그냥 문자열로 저장했던 거다.

근데 PostgreSQL로 오면서 문제가 생겼다.
PostgreSQL엔 `UUID` 네이티브 타입이 있거든.

```sql
-- 이렇게 해도 되는데...
user_id CHAR(36) NOT NULL   -- 36바이트

-- 이렇게 하면 된다
user_id UUID NOT NULL        -- 16바이트
```

UUID 자체가 128비트, 즉 16바이트짜리 데이터다.
`CHAR(36)`은 그걸 `550e8400-e29b-41d4-a716-446655440000` 형태로
사람이 읽기 좋게 풀어쓴 것뿐이다.
같은 정보를 PostgreSQL `UUID` 타입으로 저장하면 절반 크기고,
인덱스도 작아지고, 비교 연산도 문자열 비교가 아닌 바이너리 비교라 빠르다.

첫 번째 수정은 간단했다. 전체 `CHAR(36)`을 `UUID`로 바꿨다.

---

## 그럼 그냥 BIGSERIAL 쓰면 안 되나?

UUID로 바꾸고 나니 다음 질문이 생겼다.
*그냥 1, 2, 3... 순서대로 증가하는 BIGSERIAL 쓰면 안 되나?*

성능만 보면 BIGSERIAL이 낫다. 근데 API에 ID를 그대로 노출하는 순간 문제가 생긴다.

```
GET /api/posts/1
GET /api/posts/2
GET /api/posts/3   ← 이거 보면 총 게시글 수 대충 보임, 다음 ID도 예측 가능
```

순차 ID는 공격자가 ID를 순서대로 요청해서 전체 데이터를 긁어가는
열거 공격(enumeration attack)에 취약하다.
UUID는 랜덤이라 예측이 불가능하니 이 문제가 없다.

추가로 분산 환경에서 여러 서버가 각자 ID를 만들 수 있다는 것,
INSERT 전에 클라이언트가 미리 ID를 알 수 있다는 것도 장점이다.

그래서 UUID를 쓰기로 했는데, 문제는 이제부터였다.

---

## UUID v4의 함정 — 페이지 스플릿

UUID v4는 완전한 랜덤 값이다.
이게 B-Tree 인덱스를 쓰는 PostgreSQL에서 문제가 된다.

B-Tree 인덱스는 키 값을 정렬된 순서로 페이지(블록, 기본 8KB)에 저장한다.
BIGSERIAL처럼 순차 증가하는 키는 항상 인덱스의 맨 끝 페이지에 붙는다.
기존 페이지를 건드리지 않으니 빠르다.

UUID v4는 다르다.

```
[페이지 A: 0x1... ~ 0x5...]  [페이지 B: 0x6... ~ 0x9...]

새 UUID v4: 0x3...가 생성됨
→ 페이지 A 중간에 들어가야 하는데 꽉 차 있음
→ 페이지 A를 두 개로 쪼갬  ← 페이지 스플릿
→ 데이터 재배치 + 상위 노드 업데이트
→ INSERT마다 이게 반복될 수 있음
```

결과는 쓰기 성능 저하와 인덱스 단편화다.
소규모에선 체감이 없지만 수백만 건이 쌓이기 시작하면 실제 문제가 된다.

*UUID의 장점은 가져가면서 이 문제를 피할 방법이 없을까.*

---

## UUID v7 — 두 마리 토끼

UUID v7이 2024년 5월 RFC 9562로 공식 표준화됐다.
아이디어는 단순하다. **앞 48비트를 타임스탬프로 채운다.**

```
UUID v4: 550e8400-e29b-41d4-a716-446655440000
         └──────────────────────────────────┘
                     완전 랜덤

UUID v7: 018f5e6a-1b2c-7def-8abc-123456789012
         └──────┘
         현재 시각 (밀리초 단위 타임스탬프)
```

나중에 만들수록 앞부분 타임스탬프 값이 커지니까,
새로 생성되는 UUID가 항상 인덱스 끝 근처에 삽입된다.
BIGSERIAL처럼 순차 증가하는 구조라 페이지 스플릿이 거의 없다.
UUID가 주는 보안 장점은 그대로고.

---

## PostgreSQL 설정

`pg_uuidv7` 확장을 설치하면 된다.

```sql
CREATE EXTENSION IF NOT EXISTS pg_uuidv7;

CREATE TABLE users (
    user_id UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    email   VARCHAR(255) NOT NULL,
    ...
);
```

---

## Spring Boot / JPA에서 쓰기

JPA의 `@GeneratedValue(strategy = GenerationType.UUID)`는 Hibernate가
내부적으로 UUID v4를 생성한다. v7을 쓰려면 Java에서 직접 만들어야 한다.

`uuid-creator` 라이브러리를 쓰면 한 줄이다.

```groovy
// build.gradle
implementation 'com.github.f4b6a3:uuid-creator:5.3.7'
```

```java
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    private UUID id = UuidCreator.getTimeOrderedEpoch(); // 객체 생성 시 UUID v7 즉시 할당
}
```

`new User()` 하는 순간 이미 UUID v7이 박혀있다.
`@GeneratedValue` 없이도 완전히 안전하고,
INSERT 전에 ID를 알 수 있다는 UUID의 장점도 그대로 살아있다.

---

## 결론

| | BIGSERIAL | UUID v4 | UUID v7 |
|---|---|---|---|
| 페이지 스플릿 | 없음 | 있음 | 거의 없음 |
| API 보안 | 취약 | 안전 | 안전 |
| 분산 ID 생성 | 불가 | 가능 | 가능 |
| 생성 순서 정렬 | 가능 | 불가 | 가능 |
| PostgreSQL 기본 지원 | O | O | 확장 필요 |

UUID v4의 페이지 스플릿 문제를 알고 나서 BIGSERIAL로 돌아갈까도 했는데,
API 보안 때문에 UUID는 포기할 수 없었다.
UUID v7이 딱 그 지점을 해결해줬다.

`pg_uuidv7` 확장 하나 추가하는 게 전부다. 안 쓸 이유가 없었다.
