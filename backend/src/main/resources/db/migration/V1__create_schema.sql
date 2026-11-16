-- 분류: 대(1) · 중(2) · 소(3) 를 한 테이블에 계층으로 저장한다.
CREATE TABLE category (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    level       INT          NOT NULL,              -- 1: 대분류, 2: 중분류, 3: 소분류
    parent_id   BIGINT       NULL,                  -- 대분류는 NULL
    description VARCHAR(200) NULL,                  -- 목록 카드에 보이는 한 줄 소개
    sort_order  INT          NOT NULL DEFAULT 0,    -- 목록 표시 순서
    active      BOOLEAN      NOT NULL DEFAULT TRUE, -- FALSE면 화면에서 '준비 중'(클릭 불가)
    PRIMARY KEY (id),
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id)
);
CREATE INDEX idx_category_parent ON category (parent_id, sort_order);
CREATE INDEX idx_category_level ON category (level, sort_order);

-- 학습 콘텐츠: 소분류 1개당 1개. 설명은 마크다운 텍스트.
CREATE TABLE cs_content (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    minor_category_id BIGINT       NOT NULL,
    title             VARCHAR(100) NOT NULL,
    explanation       TEXT         NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_content_minor UNIQUE (minor_category_id),
    CONSTRAINT fk_content_minor FOREIGN KEY (minor_category_id) REFERENCES category (id)
);

-- 영상·대체 이미지: 파일은 객체 스토리지(S3)에 두고 URL만 저장한다. (TC-007)
-- '/media/...' 처럼 '/'로 시작하면 서버 설정 MEDIA_BASE_URL 이 앞에 붙고,
-- 'https://...' 전체 주소면 그대로 사용된다.
CREATE TABLE video (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    content_id         BIGINT       NOT NULL,
    video_url          VARCHAR(500) NOT NULL,
    fallback_image_url VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_video_content UNIQUE (content_id),
    CONSTRAINT fk_video_content FOREIGN KEY (content_id) REFERENCES cs_content (id)
);

-- 조회수: 중분류 + 날짜별 누적 1행.
CREATE TABLE daily_view_count (
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    medium_category_id BIGINT NOT NULL,
    view_date          DATE   NOT NULL,
    view_count         BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_view_medium_date UNIQUE (medium_category_id, view_date),
    CONSTRAINT fk_view_medium FOREIGN KEY (medium_category_id) REFERENCES category (id)
);
CREATE INDEX idx_view_date ON daily_view_count (view_date);

-- 운영자 계정: 비밀번호는 BCrypt 해시만 저장한다. (QC-004)
CREATE TABLE admin (
    id            VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    PRIMARY KEY (id)
);
