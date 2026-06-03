-- 创建数据库
-- 创建库
create
database if not exists ai_passage_creator;
use
ai_passage_creator;

-- 用户表
create table if not exists user
(
    id
    bigint
    auto_increment
    comment
    'id'
    primary
    key,
    userAccount
    varchar
(
    256
) not null comment '账号',
    userPassword varchar
(
    512
) not null comment '密码',
    userName varchar
(
    256
) null comment '用户昵称',
    userAvatar varchar
(
    1024
) null comment '用户头像',
    userProfile varchar
(
    512
) null comment '用户简介',
    userRole varchar
(
    256
) default 'user' not null comment '用户角色：user/admin',
    editTime datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete tinyint default 0 not null comment '是否删除',
    UNIQUE KEY uk_userAccount
(
    userAccount
),
    INDEX idx_userName
(
    userName
)
    ) comment '用户表' collate = utf8mb4_unicode_ci;

-- 文章表
create table if not exists article
(
    id              bigint auto_increment comment 'id' primary key,
    taskId          varchar(64)                        not null comment '任务ID（UUID）',
    userId          bigint                             not null comment '用户ID',
    topic           varchar(500)                       not null comment '选题',
    mainTitle       varchar(200)                       null comment '主标题',
    subTitle        varchar(300)                       null comment '副标题',
    outline         json                               null comment '大纲（JSON格式）',
    content         text                               null comment '正文（Markdown格式）',
    fullContent     text                               null comment '完整图文（Markdown格式，含配图）',
    coverImage      varchar(512)                       null comment '封面图 URL',
    images          json                               null comment '配图列表（JSON数组）',
    status          varchar(20) default 'PENDING'      not null comment '状态：PENDING/PROCESSING/COMPLETED/FAILED',
    errorMessage    text                               null comment '错误信息',
    createTime      datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    completedTime   datetime                           null comment '完成时间',
    updateTime      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete        tinyint     default 0              not null comment '是否删除',
    UNIQUE KEY uk_taskId (taskId),
    INDEX idx_userId (userId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime),
    INDEX idx_userId_status (userId, status)
    ) comment '文章表' collate = utf8mb4_unicode_ci;

-- 为 article 表添加 style 字段（文章风格）
ALTER TABLE article
    ADD COLUMN style VARCHAR(20) NULL COMMENT '文章风格：tech/emotional/educational/humorous' AFTER topic;

-- 为 article 表添加阶段相关字段
ALTER TABLE article
    ADD COLUMN phase VARCHAR(50) DEFAULT 'PENDING' COMMENT '当前阶段：PENDING/TITLE_GENERATING/TITLE_SELECTING/OUTLINE_GENERATING/OUTLINE_EDITING/CONTENT_GENERATING' AFTER status,
    ADD COLUMN titleOptions JSON NULL COMMENT '标题方案列表（3-5个方案）' AFTER subTitle,
    ADD COLUMN userDescription TEXT NULL COMMENT '用户补充描述' AFTER topic,
    ADD COLUMN enabledImageMethods JSON NULL COMMENT '允许的配图方式列表' AFTER userDescription;


-- 扩展 user 表，添加会员相关字段
ALTER TABLE user
    ADD COLUMN vipTime DATETIME NULL COMMENT '成为会员时间';

-- 创建支付记录表
CREATE TABLE IF NOT EXISTS payment_record (
                                              id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                                              userId BIGINT NOT NULL COMMENT '用户ID',
                                              stripeSessionId VARCHAR(128) COMMENT 'Stripe Checkout Session ID',
    stripePaymentIntentId VARCHAR(128) COMMENT 'Stripe 支付意向ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额（美元）',
    currency VARCHAR(8) DEFAULT 'usd' COMMENT '货币',
    status VARCHAR(32) NOT NULL COMMENT '状态：PENDING/SUCCEEDED/FAILED/REFUNDED',
    productType VARCHAR(32) NOT NULL COMMENT '产品类型：VIP_PERMANENT',
    description VARCHAR(256) COMMENT '描述',
    refundTime DATETIME NULL COMMENT '退款时间',
    refundReason VARCHAR(512) NULL COMMENT '退款原因',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    INDEX idx_userId (userId),
    INDEX idx_stripeSessionId (stripeSessionId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
    ) COMMENT '支付记录表' COLLATE = utf8mb4_unicode_ci;

-- 创建退款补偿失败任务表
CREATE TABLE IF NOT EXISTS refund_compensation_task (
                                                        id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
                                                        eventId VARCHAR(64) NOT NULL COMMENT '事件ID',
    userId BIGINT NOT NULL COMMENT '用户ID',
    paymentRecordId BIGINT NOT NULL COMMENT '支付记录ID',
    stripePaymentIntentId VARCHAR(128) NOT NULL COMMENT 'Stripe支付意向ID',
    reason VARCHAR(512) NOT NULL COMMENT '退款原因',
    source VARCHAR(64) NOT NULL COMMENT '事件来源',
    status VARCHAR(32) NOT NULL COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED',
    retryCount INT DEFAULT 0 NOT NULL COMMENT '数据库补偿重试次数',
    nextRetryTime DATETIME NULL COMMENT '下次重试时间',
    lastError VARCHAR(512) NULL COMMENT '最近一次错误信息',
    processingStartTime DATETIME NULL COMMENT '开始处理时间',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_eventId (eventId),
    INDEX idx_status_nextRetryTime (status, nextRetryTime),
    INDEX idx_paymentRecordId (paymentRecordId),
    INDEX idx_userId (userId)
    ) COMMENT '退款补偿失败任务表' COLLATE = utf8mb4_unicode_ci;

-- 添加 quota 字段（如果不存在）
ALTER TABLE user ADD COLUMN quota int default 5 not null comment '剩余配额' AFTER userRole;

-- 为已有用户设置默认配额
UPDATE user SET quota = 5 WHERE quota IS NULL;



