# chapchu-api 팀원 온보딩

## 1. 레포 클론 및 환경 준비

```bash
git clone https://github.com/chapchu-trip-platform/chapchu-api.git
cd chapchu-api
git checkout dev
```

### 필수 설치
- Java 25 (`sdk install java 25-open` 또는 직접 설치)
- Docker (로컬 PostgreSQL 실행용)

### pre-commit 훅 설치
```bash
pip install pre-commit
pre-commit install
chmod +x scripts/post-merge
cp scripts/post-merge .git/hooks/post-merge
```

### 로컬 DB 실행 (pg_uuidv7 + pgvector 필수)
```bash
docker run -d \
  --name pettrip-db \
  -e POSTGRES_DB=pettrip \
  -e POSTGRES_USER=pettrip \
  -e POSTGRES_PASSWORD=pettrip \
  -p 5432:5432 \
  pgvector/pgvector:pg16

# 확장 설치 (최초 1회)
docker exec -it pettrip-db psql -U pettrip -d pettrip \
  -c "CREATE EXTENSION IF NOT EXISTS \"pg_uuidv7\"; CREATE EXTENSION IF NOT EXISTS \"vector\";"
```

### application-local.yml 생성 (gitignore 대상, 직접 생성 필요)
```yaml
# app/src/main/resources/application-local.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pettrip
    username: pettrip
    password: pettrip
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: true
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:9000

spring.cloud.aws:
  credentials:
    access-key: test
    secret-key: test
  region:
    static: ap-northeast-2
```

## 2. 빌드 검증
```bash
./gradlew spotlessApply check
# BUILD SUCCESSFUL 확인
```

## 3. 개발 워크플로우

```
main (배포) ← PR + 리뷰 1명 ← dev (통합) ← PR ← feature/{이름}/{기능}
```

- 작업 시작: `git checkout dev && git pull && git checkout -b feature/yeonseung/user-register`
- 커밋 전: pre-commit 훅이 자동으로 Spotless + Checkstyle + SpotBugs + PMD + ArchUnit 실행
- PR 대상: 항상 `dev` 브랜치

## 4. 반드시 읽어야 할 문서
| 문서 | 내용 |
|---|---|
| `CLAUDE.md` | AI 에이전트 필수 수칙 (세션 시작 체크리스트) |
| `AGENTS.md` | 하네스 규칙 전체 |
| `docs/decisions/` | 기술 결정 001~010 |
| `docs/failures/` | 하지 말아야 할 것들 |
| `docs/schema/init.sql` | DB 스키마 |
