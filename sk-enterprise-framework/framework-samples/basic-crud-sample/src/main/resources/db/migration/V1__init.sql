-- =====================================================================
-- V1__init : 초기 스키마 (PostgreSQL)
-- BaseEntity 공통 컬럼: version, created_at, updated_at, created_by, updated_by
-- =====================================================================

CREATE TABLE tb_user (
    user_id     BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    email       VARCHAR(100) NOT NULL,
    phone       VARCHAR(20),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER',
    password    VARCHAR(100),
    version     BIGINT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50),
    CONSTRAINT uk_user_email UNIQUE (email)
);

CREATE TABLE tb_product (
    product_id  BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)   NOT NULL,
    price       NUMERIC(18, 2) NOT NULL,
    stock       INTEGER        NOT NULL DEFAULT 0,
    version     BIGINT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50)
);

CREATE TABLE tb_order (
    order_id    BIGSERIAL PRIMARY KEY,
    order_no    VARCHAR(50)    NOT NULL,
    customer_id VARCHAR(50)    NOT NULL,
    product_id  VARCHAR(50)    NOT NULL,
    quantity    INTEGER        NOT NULL,
    amount      NUMERIC(18, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL DEFAULT 'CREATED',
    version     BIGINT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    created_by  VARCHAR(50),
    updated_by  VARCHAR(50),
    CONSTRAINT uk_order_no UNIQUE (order_no)
);

CREATE TABLE tb_outbox_event (
    event_id       BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100)  NOT NULL,
    aggregate_id   VARCHAR(100)  NOT NULL,
    event_type     VARCHAR(100)  NOT NULL,
    payload        TEXT          NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    retry_count    INTEGER       NOT NULL DEFAULT 0,
    last_error     VARCHAR(1000),
    published_at   TIMESTAMP,
    version        BIGINT,
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP,
    created_by     VARCHAR(50),
    updated_by     VARCHAR(50)
);

-- 아웃박스 릴레이 폴링용 인덱스 (PENDING 이벤트를 생성순으로 조회)
CREATE INDEX idx_outbox_status ON tb_outbox_event (status, created_at);
