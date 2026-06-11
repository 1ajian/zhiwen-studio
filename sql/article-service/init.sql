create database if not exists ai_article default character set utf8mb4 collate utf8mb4_unicode_ci;
use ai_article;

create table if not exists article
(
    id bigint auto_increment comment 'id' primary key,
    taskId varchar(64) not null comment '任务ID（UUID）',
    userId bigint not null comment '用户ID',
    topic varchar(500) not null comment '选题',
    userDescription text null comment '用户补充描述',
    style varchar(20) null comment '文章风格：tech/emotional/educational/humorous',
    mainTitle varchar(200) null comment '主标题',
    subTitle varchar(300) null comment '副标题',
    titleOptions json null comment '标题方案列表（3-5个方案）',
    outline json null comment '大纲（JSON格式）',
    content text null comment '正文（Markdown格式）',
    fullContent text null comment '完整图文（Markdown格式，含配图）',
    coverImage varchar(512) null comment '封面图 URL',
    images json null comment '配图列表（JSON数组）',
    status varchar(20) default 'PENDING' not null comment '状态：PENDING/PROCESSING/COMPLETED/FAILED',
    phase varchar(50) default 'PENDING' comment '当前阶段',
    enabledImageMethods json null comment '允许的配图方式列表',
    errorMessage text null comment '错误信息',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    completedTime datetime null comment '完成时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete tinyint default 0 not null comment '是否删除',
    unique key uk_taskId (taskId),
    index idx_userId (userId),
    index idx_status (status),
    index idx_createTime (createTime),
    index idx_userId_status (userId, status)
) comment '文章表' collate = utf8mb4_unicode_ci;

create table if not exists agent_log
(
    id bigint auto_increment comment 'id' primary key,
    taskId varchar(64) not null comment '任务ID',
    agentName varchar(50) not null comment '智能体名称',
    startTime datetime not null comment '开始时间',
    endTime datetime null comment '结束时间',
    durationMs int null comment '耗时（毫秒）',
    status varchar(20) not null comment '状态：SUCCESS/FAILED',
    errorMessage text null comment '错误信息',
    prompt text null comment '使用的 Prompt',
    inputData json null comment '输入数据（JSON格式）',
    outputData json null comment '输出数据（JSON格式）',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete tinyint default 0 not null comment '是否删除',
    index idx_taskId (taskId),
    index idx_agentName (agentName),
    index idx_status (status),
    index idx_createTime (createTime)
) comment '智能体执行日志表' collate = utf8mb4_unicode_ci;
