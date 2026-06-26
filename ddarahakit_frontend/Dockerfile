# ===== 빌드 스테이지 =====
FROM node:20-alpine AS build
WORKDIR /app

# 의존성 설치 (lockfile 기반 재현 가능 설치)
COPY package.json package-lock.json ./
RUN npm ci

# ── 빌드 시 주입되는 프론트 환경값 ──────────────────────────────
# .env.production 을 git/이미지에 두지 않고 build-arg 로 주입한다.
#   docker compose:  build.args 로 전달 (루트 .env 공급)
#   CI(Kaniko):      --build-arg VITE_xxx=... 로 전달
# VITE_ 변수는 `vite build` 시 import.meta.env 로 번들에 인라인된다(클라이언트 공개값).
# 구조/공개 성격 값은 합리적 기본값을 두고, 환경별 식별자/키는 주입 필수.
ARG VITE_BASE_URL="/"
ARG VITE_API_BASE_URL="https://api.ddarahakit.com"
ARG VITE_IMG_BASE_URL="https://api.ddarahakit.com"
ARG VITE_MENTORING_WS_URL=""
ARG VITE_LOGIN_VALIDITY_TIME="86400000"
ARG VITE_LOGIN_EXTRA_TIME="60000"
ARG VITE_API_TIMEOUT="60000"
ARG VITE_STORAGE_SECRET_KEY
ARG VITE_PORTONE_STORE_ID
ARG VITE_PORTONE_CHANNEL_KEY

# vite 는 build 시 process.env 의 VITE_* 를 자동으로 import.meta.env 에 포함한다.
ENV VITE_BASE_URL=$VITE_BASE_URL \
    VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_IMG_BASE_URL=$VITE_IMG_BASE_URL \
    VITE_MENTORING_WS_URL=$VITE_MENTORING_WS_URL \
    VITE_LOGIN_VALIDITY_TIME=$VITE_LOGIN_VALIDITY_TIME \
    VITE_LOGIN_EXTRA_TIME=$VITE_LOGIN_EXTRA_TIME \
    VITE_API_TIMEOUT=$VITE_API_TIMEOUT \
    VITE_STORAGE_SECRET_KEY=$VITE_STORAGE_SECRET_KEY \
    VITE_PORTONE_STORE_ID=$VITE_PORTONE_STORE_ID \
    VITE_PORTONE_CHANNEL_KEY=$VITE_PORTONE_CHANNEL_KEY

# 소스 복사 후 프로덕션 빌드 (.env* 는 .dockerignore 로 제외 → 위 ENV 값 사용)
COPY . .
RUN npm run build

# ===== 런타임 스테이지 =====
FROM nginx:1.27-alpine
# 정적 빌드 결과물 서빙 + SPA(history 모드) 라우팅 fallback + 정적 자산 캐시.
COPY --from=build /app/dist /usr/share/nginx/html
RUN printf 'server {\n\
  listen 80;\n\
  server_name _;\n\
  root /usr/share/nginx/html;\n\
  index index.html;\n\
  location / {\n\
    try_files $uri $uri/ /index.html;\n\
  }\n\
  location /assets/ {\n\
    expires 1y;\n\
    add_header Cache-Control "public, immutable";\n\
  }\n\
}\n' > /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
