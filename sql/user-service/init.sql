create database if not exists ai_user default character set utf8mb4 collate utf8mb4_unicode_ci;
use ai_user;

create table if not exists user
(
    id bigint auto_increment comment 'id' primary key,
    userAccount varchar(256) not null comment '账号',
    userPassword varchar(512) not null comment '密码',
    userName varchar(256) null comment '用户昵称',
    userAvatar varchar(1024) null comment '用户头像',
    userProfile varchar(512) null comment '用户简介',
    userRole varchar(256) default 'user' not null comment '用户角色：user/admin/vip',
    quota int default 5 not null comment '剩余配额',
    vipTime datetime null comment '成为会员时间',
    editTime datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete tinyint default 0 not null comment '是否删除',
    unique key uk_userAccount (userAccount),
    index idx_userName (userName),
    index idx_userRole (userRole)
) comment '用户表' collate = utf8mb4_unicode_ci;

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
