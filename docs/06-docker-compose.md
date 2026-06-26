# 06. 로컬 인프라 — docker-compose

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [04. DB 스키마](04-database-schema.md) · [07. 패키지 구조](07-package-structure.md)

단일 PC에서 전체 스택을 띄우는 정의. 인프라(MariaDB·Kafka·Eureka)를 먼저, 게이트웨이/서비스가 `depends_on`으로 뒤따른다.

## 포트 맵
| 컴포넌트 | 내부 | 호스트 노출 | 비고 |
|---|---|---|---|
| gateway | 8080 | **8080** | 유일한 공개 진입점 |
| eureka | 8761 | 8761 | 대시보드 |
| kafka | 9092 | 9092 | KRaft 단일 브로커 |
| kafka-ui | 8080 | 8090 | 토픽/lag 가시화(선택) |
| mariadb | 3306 | 3306 | DB 클라이언트(로컬 전용) |
| identity/course/commerce/community/review | 8080 | (미노출) | 게이트웨이 경유만 |

> 서비스는 호스트에 포트를 노출하지 않는다(게이트웨이만 공개). Eureka `lb://` 디스커버리로 통신.

---

## docker-compose.yml

```yaml
name: ddarahakit-msa

x-svc-env: &svc-env                       # 서비스 공통 환경
  SPRING_PROFILES_ACTIVE: docker
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka:8761/eureka/
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092

services:
  # ---------- 인프라 ----------
  mariadb:
    image: mariadb:11.4
    container_name: msa-mariadb
    environment:
      MARIADB_ROOT_PASSWORD: ${DB_ROOT_PASSWORD}
      MARIADB_USER: ${DB_USER}
      MARIADB_PASSWORD: ${DB_PASSWORD}
      TZ: Asia/Seoul
    command: ["--character-set-server=utf8mb4","--collation-server=utf8mb4_unicode_ci"]
    ports: ["3306:3306"]
    volumes:
      - mariadb_data:/var/lib/mysql
      - ./infra/db/init:/docker-entrypoint-initdb.d:ro   # ↓ 스키마 5개 생성 스크립트
    healthcheck:
      test: ["CMD-SHELL","mariadb-admin ping -h127.0.0.1 -uroot -p\"$$MARIADB_ROOT_PASSWORD\" || exit 1"]
      interval: 10s, timeout: 5s, retries: 12
    networks: [msa-net]

  kafka:
    image: bitnami/kafka:3.7                # KRaft (Zookeeper 불필요)
    container_name: msa-kafka
    environment:
      KAFKA_CFG_NODE_ID: "1"
      KAFKA_CFG_PROCESS_ROLES: "broker,controller"
      KAFKA_CFG_CONTROLLER_QUORUM_VOTERS: "1@kafka:9093"
      KAFKA_CFG_LISTENERS: "PLAINTEXT://:9092,CONTROLLER://:9093"
      KAFKA_CFG_ADVERTISED_LISTENERS: "PLAINTEXT://kafka:9092"
      KAFKA_CFG_CONTROLLER_LISTENER_NAMES: "CONTROLLER"
      KAFKA_CFG_OFFSETS_TOPIC_REPLICATION_FACTOR: "1"
      KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE: "false"        # 토픽은 명시 생성
    ports: ["9092:9092"]
    volumes: [kafka_data:/bitnami/kafka]
    healthcheck:
      test: ["CMD-SHELL","kafka-topics.sh --bootstrap-server localhost:9092 --list || exit 1"]
      interval: 10s, timeout: 5s, retries: 20
    networks: [msa-net]

  kafka-init:                              # 토픽 1회 생성(파티션 3/복제 1)
    image: bitnami/kafka:3.7
    depends_on: { kafka: { condition: service_healthy } }
    entrypoint: ["/bin/bash","-c"]
    command:
      - |
        for t in commerce.order.v1 review.review.v1 identity.user.v1; do
          kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists \
            --topic $$t --partitions 3 --replication-factor 1
        done
    networks: [msa-net]
    restart: "no"

  kafka-ui:                                # 선택: 가시화
    image: provectuslabs/kafka-ui:latest
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
    ports: ["8090:8080"]
    depends_on: [kafka]
    networks: [msa-net]

  eureka:
    build: ./infra/discovery
    container_name: msa-eureka
    ports: ["8761:8761"]
    healthcheck:
      test: ["CMD-SHELL","curl -fs http://localhost:8761/actuator/health || exit 1"]
      interval: 10s, timeout: 5s, retries: 20
    networks: [msa-net]

  # ---------- 게이트웨이 ----------
  gateway:
    build: ./infra/gateway
    container_name: msa-gateway
    environment:
      <<: *svc-env
      JWT_SECRET: ${JWT_SECRET}
      APP_ALLOWED_ORIGINS: ${ALLOWED_ORIGINS}
    ports: ["8080:8080"]
    depends_on:
      eureka: { condition: service_healthy }
    networks: [msa-net]

  # ---------- 비즈니스 서비스 ----------
  identity-service:
    build: ./services/identity-service
    environment:
      <<: *svc-env
      DB_URL: jdbc:mariadb://mariadb:3306/identity_db
      DB_USER: ${DB_USER}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      AES_SECRET_KEY: ${AES_SECRET_KEY}
      GOOGLE_CLIENT_ID: ${GOOGLE_CLIENT_ID}    # …OAuth/메일 env
    depends_on:
      mariadb: { condition: service_healthy }
      kafka:   { condition: service_healthy }
      eureka:  { condition: service_healthy }
    networks: [msa-net]

  course-service:
    build: ./services/course-service
    environment: { <<: *svc-env, DB_URL: jdbc:mariadb://mariadb:3306/course_db, DB_USER: ${DB_USER}, DB_PASSWORD: ${DB_PASSWORD} }
    depends_on: { mariadb: {condition: service_healthy}, kafka: {condition: service_healthy}, eureka: {condition: service_healthy} }
    networks: [msa-net]

  commerce-service:
    build: ./services/commerce-service
    environment: { <<: *svc-env, DB_URL: jdbc:mariadb://mariadb:3306/commerce_db, DB_USER: ${DB_USER}, DB_PASSWORD: ${DB_PASSWORD}, PORTONE_SECRET: ${PORTONE_SECRET} }
    depends_on: { mariadb: {condition: service_healthy}, kafka: {condition: service_healthy}, eureka: {condition: service_healthy} }
    networks: [msa-net]

  community-service:
    build: ./services/community-service
    environment: { <<: *svc-env, DB_URL: jdbc:mariadb://mariadb:3306/community_db, DB_USER: ${DB_USER}, DB_PASSWORD: ${DB_PASSWORD} }
    depends_on: { mariadb: {condition: service_healthy}, kafka: {condition: service_healthy}, eureka: {condition: service_healthy} }
    networks: [msa-net]

  review-service:
    build: ./services/review-service
    environment: { <<: *svc-env, DB_URL: jdbc:mariadb://mariadb:3306/review_db, DB_USER: ${DB_USER}, DB_PASSWORD: ${DB_PASSWORD} }
    depends_on: { mariadb: {condition: service_healthy}, kafka: {condition: service_healthy}, eureka: {condition: service_healthy} }
    networks: [msa-net]

networks: { msa-net: { driver: bridge } }
volumes: { mariadb_data: {}, kafka_data: {} }
```

> YAML 단축표기(`{ ... }`)는 가독성용. 실제 파일에선 풀어 써도 무방.

---

## 스키마 초기화 — `infra/db/init/01-schemas.sql`
컨테이너 최초 기동 시 5개 스키마 생성([04 참조](04-database-schema.md)). 테이블은 각 서비스가 Flyway/`ddl-auto`로 생성.

```sql
CREATE DATABASE IF NOT EXISTS identity_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS course_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS commerce_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS community_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS review_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
GRANT ALL ON identity_db.*  TO 'ddarahakit'@'%';
GRANT ALL ON course_db.*    TO 'ddarahakit'@'%';
GRANT ALL ON commerce_db.*  TO 'ddarahakit'@'%';
GRANT ALL ON community_db.* TO 'ddarahakit'@'%';
GRANT ALL ON review_db.*    TO 'ddarahakit'@'%';
FLUSH PRIVILEGES;
```

## 기동 순서 / 명령
```bash
cp .env.example .env           # DB·JWT·OAuth·PortOne 값
docker compose up -d mariadb kafka eureka     # 인프라 먼저
docker compose up -d           # 게이트웨이 + 서비스
docker compose ps              # health 확인
```
의존: `mariadb·kafka·eureka(healthy)` → `gateway·서비스`. `kafka-init`이 토픽 생성 후 종료.

## 0단계(Strangler) 변형
스캐폴딩 초기엔 5서비스 대신 **monolith 컨테이너 1개**를 게이트웨이 뒤에 둔다(`build: ./monolith`). 라우트는 전부 `lb://monolith-service`로 시작 → 서비스 추출마다 해당 경로만 신규 서비스로 전환.
```

> 운영(가정용 PC) 배포는 기존 nginx 엣지 + Cloudflare 구성을 재사용하되, 단일 백엔드 대신 게이트웨이를 `api.ddarahakit.com` 업스트림으로 둔다.
