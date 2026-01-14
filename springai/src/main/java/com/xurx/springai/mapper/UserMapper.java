package com.xurx.springai.mapper;

import com.xurx.springai.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    /**
     * 根据userId查询用户
     */
    User selectByUserId(@Param("userId") String userId);

    /**
     * 根据username查询用户
     */
    User selectByUsername(@Param("username") String username);

    /**
     * 插入新用户
     */
    int insert(User user);

    /**
     * 更新用户信息
     */
    int update(User user);

    /**
     * 根据userId删除用户
     */
    int deleteByUserId(@Param("userId") String userId);
}
