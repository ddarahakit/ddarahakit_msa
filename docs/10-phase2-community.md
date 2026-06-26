# 10. 2단계 — community-service 추출 (완료)

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [01. 서비스 명세](01-services.md) · [03. 인증·게이트웨이](03-auth-gateway.md)

커뮤니티 도메인을 `community-service`(`community_db`)로 분리. 1단계와 달리 **타 도메인(user·course) 참조를 끊는** 첫 사례라, 세 가지 MSA 패턴을 처음 도입했다: **헤더 인증·FK 평문화+스냅샷 비정규화·동기 호출(Feign)**.

## 추출 범위
- **소유 테이블(community_db)**: `post`, `comment`, `post_scrap`, `post_tag`
- **엔드포인트**: `/community/**` 전체(목록·상세·글/댓글 CRUD·스크랩·랭킹·관련글·types·display·upload)

## 핵심 패턴 3가지

### ① 헤더 인증 (게이트웨이 신뢰)
community 는 JWT 를 검증하지 않는다. 게이트웨이가 검증 후 주입한 `X-User-Id`/`X-User-Role` 헤더를 `HeaderAuthenticationFilter` 가 읽어 SecurityContext 를 구성. (identity 는 토큰 발급자라 JWT 를 직접 다뤘지만, **다운스트림 서비스의 표준 방식은 헤더 신뢰**.)

### ② FK 평문화 + 스냅샷 비정규화
cross-schema FK 가 불가능하므로 엔티티의 `@ManyToOne User/Course/Lecture` 를 제거:
| 엔티티 | 평문 ID | 스냅샷(표시용) |
|---|---|---|
| Post | userIdx, courseIdx, lectureIdx | authorName, authorProfileImageUrl, courseName, lectureName |
| Comment | userIdx | authorName, authorProfileImageUrl |
| PostScrap | userIdx | — |

→ **조회는 스냅샷만으로 자족**(타 서비스 호출 0). PostRepository 의 `JOIN FETCH p.user/p.course` 전부 제거.

### ③ 동기 호출 (Feign → identity)
글/댓글 **작성 시점**에만 작성자 표시명이 필요 → `IdentityClient`(`@FeignClient(name="identity-service")`)가 identity 의 내부 엔드포인트 `GET /internal/users/{idx}` 를 호출해 name/image 를 스냅샷에 저장. 실패 시 "알 수 없음" 폴백.
- identity 에 `/internal/**`(permitAll, 게이트웨이 비노출) 신설.
- courseName/lectureName 은 신규 글에서 null(course-service 추출 후 이벤트 투영으로 보강 — TODO).

## 게이트웨이 라우팅
```
identity   ← /user/login·logout·token·signup·email·password·profile·check·uuid, /oauth2/**
community  ← /community/**            ◀ 2단계 추가
monolith   ← 그 외 (/course, /orders, /user/ordered·myreview·…)
```

## 데이터 이관 + 백필
```sql
-- 작성자/코스/강의 표시명을 모놀리스 조인으로 스냅샷에 채움
INSERT INTO community_db.post (... author_name, course_name, lecture_name ...)
SELECT ..., u.name, c.name, l.name
FROM ddarahakit.post p
  LEFT JOIN ddarahakit.user u   ON u.idx=p.user_idx
  LEFT JOIN ddarahakit.course c ON c.idx=p.course_idx
  LEFT JOIN ddarahakit.lecture l ON l.idx=p.lecture_idx;
-- comment(작성자 백필), post_scrap, post_tag 동일 이관
```
이관: post 26 / comment 24 / post_scrap 16 / post_tag 57. (게시글 26건 전부 author_name 채움)

## E2E 검증 (게이트웨이 http://localhost:8080)
| 호출 | 결과 |
|---|---|
| `GET /community/list` | **200** — 작성자 실명(김도현·이서윤…)·코스명(Kubernetes) 스냅샷 표시 |
| `POST /community/post` (로그인 쿠키) | **200** — X-User-Id 로 작성, **Feign 으로 작성자명(심준보) 스냅샷** 저장 |
| `GET /course/list`·`/user/profile`·`/user/myquestion` | **200** (모놀리스·identity 회귀 OK) |

community-service 는 Eureka 에 `COMMUNITY-SERVICE` 등록, 게이트웨이가 `lb://` 로 호출, community 는 `lb://identity-service` 로 Feign 호출.

## 다음(3단계) — commerce-service
orders+cart 분리(`commerce_db`). course 가격 동기검증(Feign) + 결제완료 시 **`OrderPaid` 이벤트(Kafka)** 발행 → course(추후) enrollment. **첫 이벤트 발행/아웃박스** 도입 지점.
