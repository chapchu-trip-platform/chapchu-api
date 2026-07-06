# 006. 엔드포인트 역할 배분

## 상태
- [x] 확정됨 (진행 중 추가 예정)

## 역할
- **류연승**: auth, user, pet, photo
- **이정범**: weather, place, trip/course, stamp, album, keyring-card, review(작성)

## 엔드포인트 목록

### 계정
| 기능 | Method | URL | 담당 |
|---|---|---|---|
| 로그인 | POST | /auth/google | 류연승 |

### 유저
| 기능 | Method | URL | 담당 |
|---|---|---|---|
| 사용자 정보 조회 | GET | /users/me | 류연승 |
| 사용자 선호 사항 등록 | POST | /users/me/preferences | 류연승 |
| 사용자 선호 사항 조회 | GET | /users/me/preferences | 류연승 |
| 사용자 선호 사항 수정 | PATCH | /users/me/preferences | 류연승 |
| 닉네임 등록 | POST | /users/me | 류연승 |
| 닉네임 변경 | PATCH | /users/me | 류연승 |
| 회원 탈퇴 | PATCH | /users/me | 류연승 |
| 마이페이지 요약 조회 | GET | /users/me/mypage | 미정 |
| 작성한 리뷰 조회 | GET | /users/me/reviews | 미정 |

### 반려견
| 기능 | Method | URL | 담당 |
|---|---|---|---|
| 반려견 정보 조회 | GET | /pets | 류연승 |
| 반려견 정보 등록 | POST | /pets | 류연승 |
| 반려견 정보 수정 | PATCH | /pets/{petId} | 류연승 |
| 반려견 정보 삭제 | DELETE | /pets/{petId} | 류연승 |

### 공통
| 기능 | Method | URL | 담당 |
|---|---|---|---|
| 날씨 조회 | GET | /weather | 이정범 |
| 메인 페이지 조회 | GET | /main | 미정 |

### 지도 / 코스
| 기능 | Method | URL | 담당 |
|---|---|---|---|
| 장소 목록 조회 | GET | /places | 이정범 |
| 장소 상세 조회 | GET | /places/{externalPlaceId} | 이정범 |
| 여행 코스 생성 | POST | /courses | 이정범 |
| 여행 경로 조회 | GET | /courses/{courseId}/routes | 이정범 |
| 여행 코스 조회 | GET | /courses/{courseId} | 이정범 |
| 위치 기반 방문 인증 | POST | /visit-verifications | 이정범 |
| 여행 코스 완료 처리 | PATCH | /courses/{courseId}/complete | 이정범 |

### 스탬프
| 기능 | Method | URL | 담당 |
|---|---|---|---|
| 스탬프 현황 조회 | GET | /stamps/me | 이정범 |

### 사진
| 기능 | Method | URL | 담당 |
|---|---|---|---|
| 사진 업로드 URL 발급 | POST | /photos/upload-url | 류연승 |
| 사진 저장 | POST | /photos | 류연승 |

### 앨범
| 기능 | Method | URL | 담당 |
|---|---|---|---|
| 앨범 생성 | POST | /albums | 이정범 |
| 앨범 목록 조회 | GET | /albums | 이정범 |
| 앨범 상세 조회 | GET | /albums/{albumId} | 이정범 |
| 앨범 사진 등록 | POST | /albums/{albumId}/photos | 이정범 |

### 기념카드
| 기능 | Method | URL | 담당 |
|---|---|---|---|
| 기념 카드 목록 조회 | GET | /keyring-cards | 이정범 |
| 기념 카드 생성 | POST | /keyring-cards | 이정범 |
| 기념 카드 삭제 | DELETE | /keyring-cards/{cardId} | 이정범 |

### 리뷰
| 기능 | Method | URL | 담당 |
|---|---|---|---|
| 장소 리뷰 작성 | POST | /places/{externalPlaceId}/reviews | 이정범 |
| 리뷰 추천 | POST | /reviews/{reviewId}/recommendations | 미정 |
| 리뷰 추천 취소 | DELETE | /reviews/{reviewId}/recommendations | 미정 |

---

## 개발 중 추가 예정 (TBD)

### 커뮤니티 (posts, comments)
ERD에 설계되어 있으나 MVP 1차 범위 미포함. 개발 진행하며 추가.

### RAG 추천
서비스 핵심 차별점. 담당자 및 엔드포인트 미확정. 개발 진행하며 추가.
예상 URL: `GET /courses/recommendation`

## 에이전트 행동 지침
- 신규 엔드포인트 추가 시 이 문서에 반영하라.
- 미정 항목은 담당자 확정 시 업데이트하라.
