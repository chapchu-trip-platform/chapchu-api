# 014. PostgreSQL PVC 잔존 상태에서 비밀번호 변경 시 인증 실패

## 증상
```
org.postgresql.util.PSQLException: FATAL: password authentication failed for user "chapchu"
```
또는 chapchu-api 가 DB 연결을 못 하고 CrashLoopBackOff.

## 원인
PostgreSQL은 최초 기동 시 PVC에 데이터를 초기화한다 (`POSTGRES_PASSWORD` 환경변수 사용).
이후 `postgres-secret`의 비밀번호를 변경하고 Pod만 재시작해도 **PVC에 남아 있는 기존 비밀번호**로 계속 인증하므로
새 비밀번호와 불일치가 발생한다.

## 해결
비밀번호를 변경하려면 **PVC까지 함께 삭제하고 재생성**해야 한다:
```bash
kubectl delete deployment postgres -n chapchu
kubectl delete pvc postgres-pvc -n chapchu
# Secret 재생성
kubectl delete secret postgres-secret -n chapchu
kubectl create secret generic postgres-secret -n chapchu --from-literal=POSTGRES_PASSWORD=<new-password>
# 재배포
kubectl apply -f k8s/postgres.yml
```

## 에이전트 행동 지침
- `postgres-secret` 변경 필요 시 반드시 PVC까지 삭제하도록 사용자에게 안내.
- PVC 삭제 = DB 데이터 전체 소실. 운영 환경에서는 먼저 데이터 백업 필수.
