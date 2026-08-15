# 035. FE 리다이렉트 URL 단일화 및 /auth/login redirect 파라미터 지원

## 배경

BFF 패턴 전환(033) 초기에 FE 리다이렉트 경로를 두 변수로 관리했다.

```
FE_CALLBACK_URL   = https://chapchu-six.vercel.app/auth/callback  (기존 유저)
FE_ONBOARDING_URL = https://chapchu-six.vercel.app/onboarding      (신규 유저)
```

문제: 개발 환경에서는 FE가 `localhost:3000`에서 뜨는데, 배포 서버는 항상 Vercel URL로 리다이렉트한다.
FE 개발자가 로컬에서 OAuth 흐름을 테스트하려면 배포 환경을 거쳐야 했다.

또한 두 URL이 실제로는 같은 Vercel 도메인 + 다른 경로라 단일 변수로 관리할 수 있었다.

## 결정

`FE_CALLBACK_URL` + `FE_ONBOARDING_URL` → `FE_REDIRECT_URL` 하나로 통합한다.

신규/기존 유저 구분은 파라미터(`?registration_token=`, `#access_token=`)로 FE가 처리한다.
chapchu-api는 항상 같은 `FE_REDIRECT_URL`로 리다이렉트하고 파라미터만 다르게 붙인다.

### /auth/login?redirect= 파라미터

환경별 리다이렉트를 지원하기 위해 `redirect` 쿼리 파라미터를 추가한다.

```
GET /auth/login?redirect=http://localhost:3000/auth/callback
```

FE가 자신의 현재 origin을 파라미터로 넘기면, chapchu-api는 OAuth 완료 후 그 URL로 돌려보낸다.

**오픈 리다이렉트 방지:** `redirect` 값은 `cors.allowed-origins`에 등록된 origin으로 시작할 때만 허용한다.
허용되지 않은 도메인이면 기본값(`FE_REDIRECT_URL`)으로 폴백한다.

파라미터는 `oauth2_redirect` 쿠키(HttpOnly, 5분 TTL, `/auth/callback` 경로 한정)에 저장했다가
callback 시점에 읽어 사용한다.

### 로컬 개발 흐름

```js
// FE 코드
const redirect = encodeURIComponent(`${window.location.origin}/auth/callback`);
location.href = `${API_URL}/auth/login?redirect=${redirect}`;
```

배포 환경에서는 `redirect` 없이 `/auth/login`만 호출해도 `FE_REDIRECT_URL` 기본값으로 동작한다.

## 배포 설정

```yaml
# k8s/chapchu-api-configmap.yaml
FE_REDIRECT_URL: "https://chapchu-six.vercel.app/auth/callback"
CORS_ALLOWED_ORIGINS: "https://chapchu-six.vercel.app,https://api.chapchu.site,http://localhost:3000"
```

## 에이전트 행동 지침

- `FE_CALLBACK_URL`, `FE_ONBOARDING_URL` 변수는 삭제됐다. 코드에 추가하지 마라.
- `redirect` 파라미터 검증은 반드시 `cors.allowed-origins` 기준으로 한다. 임의 URL 허용은 오픈 리다이렉트다.
- 새 FE 도메인이 생기면 ConfigMap의 `CORS_ALLOWED_ORIGINS`에 추가해야 `redirect` 파라미터에서도 허용된다.
