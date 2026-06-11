create database if not exists ai_payment default character set utf8mb4 collate utf8mb4_unicode_ci;
use ai_payment;

create table if not exists payment_record
(
    id bigint auto_increment primary key comment '主键',
    userId bigint not null comment '用户ID',
    stripeSessionId varchar(128) comment 'Stripe Checkout Session ID',
    stripePaymentIntentId varchar(128) comment 'Stripe 支付意向ID',
    amount decimal(10,2) not null comment '金额（美元）',
    currency varchar(8) default 'usd' comment '货币',
    status varchar(32) not null comment '状态：PENDING/SUCCEEDED/FAILED/REFUNDED',
    productType varchar(32) not null comment '产品类型：VIP_PERMANENT',
    description varchar(256) comment '描述',
    refundTime datetime null comment '退款时间',
    refundReason varchar(512) null comment '退款原因',
    createTime datetime default CURRENT_TIMESTAMP comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    index idx_userId (userId),
    index idx_stripeSessionId (stripeSessionId),
    index idx_status (status),
    index idx_createTime (createTime)
) comment '支付记录表' collate = utf8mb4_unicode_ci;

create table if not exists refund_compensation_task
(
    id bigint auto_increment primary key comment '主键',
    eventId varchar(64) not null comment '事件ID',
    userId bigint not null comment '用户ID',
    paymentRecordId bigint not null comment '支付记录ID',
    stripePaymentIntentId varchar(128) not null comment 'Stripe 支付意向ID',
    reason varchar(512) not null comment '退款原因',
    source varchar(64) not null comment '事件来源',
    status varchar(32) not null comment '状态：PENDING/PROCESSING/SUCCESS/FAILED',
    retryCount int default 0 not null comment '数据库补偿重试次数',
    nextRetryTime datetime null comment '下次重试时间',
    lastError varchar(512) null comment '最近一次错误信息',
    processingStartTime datetime null comment '开始处理时间',
    createTime datetime default CURRENT_TIMESTAMP comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP on update CURRENT_TIMESTAMP comment '更新时间',
    unique key uk_eventId (eventId),
    index idx_status_nextRetryTime (status, nextRetryTime),
    index idx_paymentRecordId (paymentRecordId),
    index idx_userId (userId)
) comment '退款补偿失败任务表' collate = utf8mb4_unicode_ci;

create table if not exists undo_log
(
    branch_id bigint not null comment '分支事务ID',
    xid varchar(128) not null comment '全局事务ID',
    context varchar(128) not null comment '上下文',
    rollback_info longblob not null comment '回滚日志',
    log_status int not null comment '日志状态',
    log_created datetime not null comment '创建时间',
    log_modified datetime not null comment '修改时间',
    ext varchar(100) null comment '扩展信息',
    primary key (branch_id),
    unique key ux_undo_log (xid, branch_id)
) comment 'Seata AT 模式回滚日志表' collate = utf8mb4_unicode_ci;
