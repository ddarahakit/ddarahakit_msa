# 04. 서비스별 DB 스키마 분할 (DDL)

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [01. 서비스 명세](01-services.md) · [02. Kafka](02-event-driven-kafka.md)

공유 MariaDB 인스턴스에 **서비스별 스키마**를 둔다(ADR-2). 원칙:
- **cross-schema FK 금지** — 타 도메인 참조는 `*_idx` 컬럼만 남기고 FK 제약 제거.
- 같은 스키마 내부 FK는 유지.
- 각 서비스의 감사 컬럼 `create_date DATETIME(6)`, `update_date DATETIME(6)`는 공통(이하 생략 표기).
- 문자셋 `utf8mb4 / utf8mb4_unicode_ci`.
- 발행 서비스는 `outbox`, 소비 서비스는 `processed_event` 테이블 보유([02 참조](02-event-driven-kafka.md)).

> 기준: 현행 운영 스키마 `ddarahakit` 실제 컬럼 정의. **굵게 표시**된 부분이 모놀리스 대비 변경점.

```sql
CREATE DATABASE identity_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE course_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE commerce_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE community_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE review_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 1. identity_db

스키마 내부 FK(`email_verify.user_idx → user`)는 유지. 외부 절단 없음.

```sql
CREATE TABLE user (
  idx               BIGINT AUTO_INCREMENT PRIMARY KEY,
  email             VARCHAR(50)  NOT NULL UNIQUE,
  password          VARCHAR(200) NOT NULL,
  name              VARCHAR(20)  NOT NULL,
  role              VARCHAR(255) NOT NULL,
  enabled           BIT(1)       NOT NULL,
  introduction      VARCHAR(200),
  phone_number      VARCHAR(15),
  profile_image_url VARCHAR(200),
  provider          VARCHAR(255),           -- 'email' | 'google' | 'kakao'
  provider_id       VARCHAR(255),
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  UNIQUE KEY uk_provider (provider, provider_id)   -- 소셜 식별 조회 인덱스
);

CREATE TABLE refresh_token (
  idx          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_idx     BIGINT       NOT NULL,
  token_id     VARCHAR(36)  NOT NULL UNIQUE,
  token_family VARCHAR(36)  NOT NULL,         -- 회전 패밀리(탈취 감지)
  expires_at   DATETIME(6)  NOT NULL,
  revoked      BIT(1)       NOT NULL,
  used         BIT(1)       NOT NULL,
  ip_address   VARCHAR(45), user_agent VARCHAR(500),
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  KEY idx_family (token_family),
  CONSTRAINT fk_rt_user FOREIGN KEY (user_idx) REFERENCES user(idx)
);

CREATE TABLE email_verify (
  idx       BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_idx  BIGINT,
  uuid      VARCHAR(200) NOT NULL UNIQUE,
  type      VARCHAR(50)  NOT NULL,            -- SIGNUP | PASSWORD_RESET
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  CONSTRAINT fk_ev_user FOREIGN KEY (user_idx) REFERENCES user(idx)
);

CREATE TABLE outbox ( /* §아웃박스 공통 DDL */ );
```

---

## 2. course_db  (코어 + 읽기모델)

```sql
CREATE TABLE category (
  idx               BIGINT AUTO_INCREMENT PRIMARY KEY,
  name              VARCHAR(20)  NOT NULL UNIQUE,
  slug              VARCHAR(20)  NOT NULL UNIQUE,
  materialized_path VARCHAR(200) NOT NULL UNIQUE,
  parent_idx        BIGINT,
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  CONSTRAINT fk_cat_parent FOREIGN KEY (parent_idx) REFERENCES category(idx)
);

CREATE TABLE course (
  idx                 BIGINT AUTO_INCREMENT PRIMARY KEY,
  name                VARCHAR(100) NOT NULL UNIQUE,
  description         TEXT, text TEXT, image VARCHAR(200),
  level               VARCHAR(20), is_display BIT(1),
  original_price      INT NOT NULL, sale_price INT NOT NULL,
  rating1 INT NOT NULL, rating2 INT NOT NULL, rating3 INT NOT NULL,
  rating4 INT NOT NULL, rating5 INT NOT NULL,           -- ◀ review.review.v1 이벤트로 갱신
  total_reviews_count INT,                              -- ◀ 동상
  category_idx        BIGINT,                            -- 내부 FK 유지
  **owner_user_idx     BIGINT,**                         -- 🔗 was course.user_idx FK → 평문 ID
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  CONSTRAINT fk_course_cat FOREIGN KEY (category_idx) REFERENCES category(idx)
);

CREATE TABLE section (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL,
  course_idx BIGINT NOT NULL,
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  CONSTRAINT fk_sec_course FOREIGN KEY (course_idx) REFERENCES course(idx)
);

CREATE TABLE lecture (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL,
  content LONGTEXT, text LONGTEXT, video_url VARCHAR(200),
  free BIT(1) NOT NULL, play_time INT,
  section_idx BIGINT,
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  CONSTRAINT fk_lec_section FOREIGN KEY (section_idx) REFERENCES section(idx)
);

CREATE TABLE lecture_complete (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY,
  **user_idx BIGINT,**                                   -- 🔗 외부 ID(평문)
  course_idx BIGINT, lecture_idx BIGINT,                 -- 내부(course_db) 참조
  completed_at DATETIME(6),
  UNIQUE KEY uk_user_lecture (user_idx, lecture_idx),    -- ◀ 중복 완료 방지(IDOR 점검 보강)
  KEY idx_user_course (user_idx, course_idx)
);

CREATE TABLE roadmap (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL,
  description TEXT, image VARCHAR(200),
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL
);
CREATE TABLE roadmap_course (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY, sort_order INT NOT NULL,
  roadmap_idx BIGINT, course_idx BIGINT,                 -- 둘 다 course_db 내부
  CONSTRAINT fk_rc_roadmap FOREIGN KEY (roadmap_idx) REFERENCES roadmap(idx),
  CONSTRAINT fk_rc_course  FOREIGN KEY (course_idx)  REFERENCES course(idx)
);

-- ★ 신규: 수강권 읽기모델 (commerce.OrderPaid 투영)
CREATE TABLE enrollment (
  idx        BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_idx   BIGINT NOT NULL,                            -- 외부 ID
  course_idx BIGINT NOT NULL,
  order_id   BIGINT NOT NULL,                            -- 멱등/회수 추적
  granted_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_user_course (user_idx, course_idx)       -- 수강권 단일 출처
);

CREATE TABLE outbox ( /* 선택: CourseChanged 발행 시 */ );
CREATE TABLE processed_event ( /* OrderPaid/Refunded, Review* 소비 */ );
```
> `course.getOrders()`(주문수)·`course.getReviews()` 컬렉션은 제거. **주문수 = `enrollment` 카운트**, **평점 = `rating*` 집계**로 자족 응답.

---

## 3. commerce_db

```sql
CREATE TABLE orders (
  idx           BIGINT AUTO_INCREMENT PRIMARY KEY,
  **user_idx      BIGINT,**                              -- 🔗 외부 ID
  paid BIT(1), refunded BIT(1),
  payment_id    VARCHAR(255) UNIQUE,                     -- PortOne 결제 ID(멱등)
  payment_price INT NOT NULL,
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  KEY idx_user_paid (user_idx, paid)
);

CREATE TABLE orders_item (
  idx        BIGINT AUTO_INCREMENT PRIMARY KEY,
  orders_idx BIGINT,                                     -- 내부 FK
  **course_idx BIGINT,**                                 -- 🔗 외부 ID
  **course_name VARCHAR(100),**                          -- ★ 스냅샷(영수증 표시)
  **unit_price  INT NOT NULL,**                          -- ★ 결제시점 가격 스냅샷
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  CONSTRAINT fk_oi_orders FOREIGN KEY (orders_idx) REFERENCES orders(idx)
);

CREATE TABLE cart (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY,
  **user_idx BIGINT UNIQUE,**                            -- 🔗 외부 ID
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL
);
CREATE TABLE cart_item (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY,
  cart_idx BIGINT,                                       -- 내부 FK
  **course_idx BIGINT,**                                 -- 🔗 외부 ID
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  CONSTRAINT fk_ci_cart FOREIGN KEY (cart_idx) REFERENCES cart(idx),
  UNIQUE KEY uk_cart_course (cart_idx, course_idx)
);

CREATE TABLE outbox ( /* OrderPaid / OrderRefunded 발행 */ );
```

---

## 4. community_db

```sql
CREATE TABLE post (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255), content LONGTEXT, text LONGTEXT,
  post_type ENUM('QUESTION','FREE','NOTICE') NOT NULL,
  view_count INT NOT NULL,
  **user_idx BIGINT, course_idx BIGINT, lecture_idx BIGINT,**  -- 🔗 모두 외부 ID
  **author_name VARCHAR(20),**                            -- ★ 선택: 표시명 투영 캐시
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  KEY idx_type (post_type), KEY idx_course (course_idx), KEY idx_user (user_idx)
);
CREATE TABLE comment (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY,
  content LONGTEXT, text LONGTEXT, accepted BIT(1) NOT NULL,
  post_idx BIGINT,                                        -- 내부 FK
  **user_idx BIGINT,**                                    -- 🔗 외부 ID
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  CONSTRAINT fk_cmt_post FOREIGN KEY (post_idx) REFERENCES post(idx),
  KEY idx_post (post_idx)
);
CREATE TABLE post_scrap (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY,
  post_idx BIGINT NOT NULL,                               -- 내부 FK
  **user_idx BIGINT NOT NULL,**                           -- 🔗 외부 ID
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  CONSTRAINT fk_ps_post FOREIGN KEY (post_idx) REFERENCES post(idx),
  UNIQUE KEY uk_user_post (user_idx, post_idx)
);
CREATE TABLE post_tag (
  post_idx BIGINT NOT NULL, tag VARCHAR(30),              -- 내부 FK
  CONSTRAINT fk_pt_post FOREIGN KEY (post_idx) REFERENCES post(idx),
  KEY idx_tag (tag)
);
CREATE TABLE processed_event ( /* UserDeleted 등 소비 */ );
```

---

## 5. review_db

```sql
CREATE TABLE review (
  idx BIGINT AUTO_INCREMENT PRIMARY KEY,
  comment VARCHAR(100) NOT NULL, rating INT NOT NULL,
  **user_idx BIGINT NOT NULL, course_idx BIGINT NOT NULL,**  -- 🔗 외부 ID
  create_date DATETIME(6) NOT NULL, update_date DATETIME(6) NOT NULL,
  UNIQUE KEY uk_user_course (user_idx, course_idx),       -- ◀ 1인 1리뷰(점검 지적 보강)
  KEY idx_course (course_idx)
);
CREATE TABLE outbox ( /* ReviewCreated/Updated/Deleted 발행 */ );
CREATE TABLE processed_event ( /* UserDeleted 소비 */ );
```

---

## 공통 인프라 테이블 DDL

```sql
-- 발행 서비스(commerce, review, identity, course?)
CREATE TABLE outbox (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  event_id     CHAR(36)     NOT NULL UNIQUE,
  aggregate    VARCHAR(50)  NOT NULL,
  aggregate_id VARCHAR(50)  NOT NULL,
  topic        VARCHAR(100) NOT NULL,
  event_type   VARCHAR(50)  NOT NULL,
  payload      JSON         NOT NULL,
  created_at   DATETIME(6)  NOT NULL,
  published_at DATETIME(6)  NULL,
  KEY idx_unpublished (published_at, id)
);
-- 소비 서비스(course, community, review)
CREATE TABLE processed_event (
  event_id     CHAR(36)    NOT NULL,
  consumer     VARCHAR(80) NOT NULL,
  processed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (event_id, consumer)
);
```

---

## 데이터 마이그레이션(분리 시점)
1. 신규 스키마 5개 생성.
2. 모놀리스 `ddarahakit`에서 각 소유 테이블을 해당 스키마로 복사(`INSERT … SELECT` / dump).
3. **cross-schema FK DROP** → `*_idx`는 데이터 그대로(값만 유지, 제약만 제거).
4. **파생 데이터 backfill**: `enrollment`는 기존 `orders_item`(paid·미환불) 기준 생성, `orders_item.unit_price/course_name`은 당시 `course`에서 채움.
5. Strangler 단계별로 트래픽을 신규 서비스로 전환([ARCHITECTURE §8](../ARCHITECTURE.md)).
