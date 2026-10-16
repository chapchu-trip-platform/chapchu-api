# 016. Flyway 미적용 마이그레이션 직접 수정 예외 패턴

## 상황
초기 마이그레이션(V1)에 버그가 있어 Flyway가 항상 실패했다. 결과적으로 V2, V3도 한 번도 실행된 적 없었다.
V2/V3에서 동일한 버그(`uuid_generate_v7()`)가 발견됐고, 새 마이그레이션 V4/V5로는 해결 불가능했다.
(Flyway는 V4 전에 V2를 실행하므로 V2가 실패하면 V4까지 도달하지 못함)

## 원칙 vs 현실
`docs/decisions/011` 규칙: **기존 마이그레이션 파일 수정 금지**
이 규칙의 목적: Flyway 체크섬 충돌 방지 (`flyway_schema_history`에 기록된 체크섬과 파일 내용 불일치 → 기동 실패)

**예외 조건**: 해당 파일이 `flyway_schema_history`에 단 한 번도 성공 기록이 없는 경우
→ 체크섬 레코드 자체가 없으므로 충돌 발생 불가 → 직접 수정이 기술적으로 안전

## 적용 절차
1. DB 접속 후 레코드 없음 확인:
   ```sql
   SELECT * FROM flyway_schema_history WHERE version IN ('2', '3');
   -- 결과: 0 rows → 직접 수정 가능
   ```
2. 파일 직접 수정
3. `docs/schema/init.sql` 동기화
4. PR 설명에 예외 근거 명시

## 재발 방지
- 새 마이그레이션 작성 시 로컬에서 `./gradlew :app:bootRun`으로 Flyway 성공 여부 확인 후 커밋
- V1이 성공했다면 V2/V3도 반드시 성공했을 것 → V1 버그가 근본 원인이었음
