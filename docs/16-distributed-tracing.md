# 16. 분산 추적 (Micrometer Tracing → Zipkin) + Kafka 추적 (완료)

[← ARCHITECTURE.md](../ARCHITECTURE.md) · 관련: [02. Kafka 이벤트](02-event-driven-kafka.md)

서비스 간 호출(동기 Feign·비동기 Kafka)을 **하나의 trace 로 연결**해 Zipkin 에서 흐름과 토폴로지를 본다.

## 구성
- 각 서비스(+게이트웨이)에 `micrometer-tracing-bridge-brave` + `zipkin-reporter-brave` + **`feign-micrometer`**(Feign 클라이언트 관측). root `build.gradle` 의 `subprojects`(common 제외).
- `management.tracing.sampling.probability=1.0`, `propagation.type=w3c`, `management.zipkin.tracing.endpoint=http://zipkin:9411/api/v2/spans`.
- `docker-compose.msa.yml` 에 `openzipkin/zipkin`(9411) + 전 서비스 `ZIPKIN_ENDPOINT` env.

## 동기 전파 (Feign)
- `feign-micrometer` 가 없으면 Feign 호출이 관측되지 않아 **trace 가 끊긴다(서비스별 독립 trace)** — 실제로 처음엔 끊겼고, 의존성 추가로 해결.
- 이제 commerce→course(가격검증), review→course(존재확인), review→identity(작성자), course→review(상세 리뷰)가 한 trace 로 이어진다.

## 비동기 전파 (Kafka, 아웃박스 경유) ★
아웃박스 릴레이는 별도 스케줄에서 돌아 **원 요청의 trace 컨텍스트가 사라진다.** 이를 잇기 위해:
1. **OutboxAppender**: append(비즈니스 트랜잭션 내, 원 요청 span 활성) 시점에 현재 span 의 `traceparent`(`00-{traceId}-{spanId}-01`)를 캡처해 outbox 행에 저장.
2. **OutboxRelay**: 발행 시 그 `traceparent` 를 Kafka record 헤더로 싣는다(`ProducerRecord.headers().add("traceparent", ...)`). 발행자 observation 은 끔(헤더 덮어쓰기 방지).
3. **소비자(course)**: `spring.kafka.listener.observation-enabled=true` → record 의 `traceparent` 를 추출해 **원 요청 trace 를 이어서** 소비 span 생성.

→ 결과: `리뷰 작성(review) → ReviewCreated → Kafka → course 소비(평점 투영)` 가 **하나의 trace**. (의존 그래프의 `review→course` callCount 가 동기+비동기 합산으로 잡힘)

## 검증 (Zipkin http://localhost:9411)
- `/api/v2/services` → 5개 서비스 추적 등록.
- **의존 그래프**(`/api/v2/dependencies`):
  ```
  review-service   → course-service   (2)   # Feign 존재확인 + ReviewCreated 비동기 소비
  review-service   → identity-service (1)   # Feign 작성자
  commerce-service → course-service   (1)   # Feign 가격검증
  ```
- 횡단 trace 예: `[commerce+course]`, `[course+review]`, `[course+identity+review]`(리뷰작성 동기+비동기).

## Zipkin vs Kiali (요약)
- Zipkin 의 **의존 그래프 = "누가 누구를 호출"** 토폴로지(Kiali 의 그래프에 해당). 단 **실시간 트래픽 애니메이션·health·mTLS 는 없음**(그건 Istio+Kiali 영역).
- trace 타임라인(waterfall)로 각 구간 지연까지 확인 가능.

## 알려진 한계 / 후속
- **게이트웨이 span 미노출**: 현재 Zipkin services 에 gateway 없음(리액티브 게이트웨이 추적 export 미동작) → gateway→service 엣지 미표시. 인터‑서비스 토폴로지는 정상. (후속: 리액티브 컨텍스트 전파 점검)
- **릴레이 폴링 노이즈**: `task outbox-relay.publish` span 이 매 1초 생성돼 trace 목록을 채움 → Zipkin UI 에서 spanName/서비스로 필터링하거나 릴레이 주기를 늘려 완화 가능.
- 운영 전: 샘플링 확률을 1.0→0.1 등으로 낮춰 오버헤드 관리.
