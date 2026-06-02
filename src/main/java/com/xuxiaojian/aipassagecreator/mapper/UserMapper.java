package com.xuxiaojian.aipassagecreator.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xuxiaojian.aipassagecreator.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * ClassName: UserMapper
 * Package: com.xuxiaojian.aipassagecreator.mapper
 * Description:
 *
 * @Author 阿健
 * @Create 2026-05-26 20:24
 * @Version 1.0
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Update("UPDATE user SET quota = quota - 1 WHERE id = #{userId} AND quota > 0")
    int decrementQuota(Long userId);

    @Update("UPDATE user SET vipTime = null , userRole = 'user' WHERE id = #{user.id}")
    Integer refundUpdateUser(@Param("user") User updateUser);
}
