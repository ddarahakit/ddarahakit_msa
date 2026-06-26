-- MSA 서비스별 스키마 생성 (공유 MariaDB 인스턴스, 스키마 분리)
-- 0단계: 모놀리스용 `ddarahakit` 스키마도 함께 둔다(Strangler 전환 동안 공존).
CREATE DATABASE IF NOT EXISTS ddarahakit   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;  -- 모놀리스(0단계)
CREATE DATABASE IF NOT EXISTS identity_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS course_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS commerce_db  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS community_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS review_db    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS mentoring_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- compose 의 MARIADB_USER 에 전 스키마 권한 부여
GRANT ALL PRIVILEGES ON ddarahakit.*   TO 'ddarahakit'@'%';
GRANT ALL PRIVILEGES ON identity_db.*  TO 'ddarahakit'@'%';
GRANT ALL PRIVILEGES ON course_db.*    TO 'ddarahakit'@'%';
GRANT ALL PRIVILEGES ON commerce_db.*  TO 'ddarahakit'@'%';
GRANT ALL PRIVILEGES ON community_db.* TO 'ddarahakit'@'%';
GRANT ALL PRIVILEGES ON review_db.*    TO 'ddarahakit'@'%';
GRANT ALL PRIVILEGES ON mentoring_db.* TO 'ddarahakit'@'%';
FLUSH PRIVILEGES;
