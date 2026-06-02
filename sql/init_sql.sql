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



