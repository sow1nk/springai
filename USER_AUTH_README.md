# 用户认证和数据持久化功能说明

## 功能概述

本次更新实现了以下功能：
1. 用户唯一标识（UUID）管理
2. JWT令牌认证
3. 用户信息和聊天记录持久化到MySQL
4. MyBatis数据访问层

## 数据库表结构

### 1. 用户表 (user)
```sql
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` VARCHAR(64) NOT NULL UNIQUE COMMENT '用户唯一标识UUID',
    `username` VARCHAR(50) COMMENT '用户名',
    `email` VARCHAR(100) COMMENT '邮箱',
    `avatar` VARCHAR(255) COMMENT '头像URL',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 2. 聊天记录表 (chat_record)
```sql
CREATE TABLE IF NOT EXISTS `chat_record` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色：user/assistant/system',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `model` VARCHAR(50) COMMENT '使用的模型',
    `tokens` INT COMMENT '消耗的token数',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天记录表';
```

## 初始化数据库

在MySQL中执行以下命令创建表：
```bash
mysql -u root -p mysql_prac < springai/src/main/resources/sql/schema-user.sql
```

## 后端实现

### 1. 实体类
- `User.java`: 用户实体
- `ChatRecord.java`: 聊天记录实体

### 2. Mapper层
- `UserMapper.java` / `UserMapper.xml`: 用户数据访问
- `ChatRecordMapper.java` / `ChatRecordMapper.xml`: 聊天记录数据访问

### 3. Service层
- `UserService.java`: 用户业务逻辑
- `ChatRecordService.java`: 聊天记录业务逻辑

### 4. Controller层
- `AuthController.java`: 认证接口（登录、注册、验证令牌）
- `ChatController.java`: 聊天接口（已更新支持userId）

### 5. 工具类
- `JwtUtils.java`: JWT令牌生成和解析

## API接口

### 认证接口

#### 1. 登录/注册
```
POST /api/auth/login
Content-Type: application/json

{
  "deviceId": "用户的UUID"
}

响应:
{
  "token": "JWT令牌",
  "userId": "用户ID",
  "username": "用户名"
}
```

#### 2. 验证令牌
```
GET /api/auth/validate
Authorization: Bearer {token}

响应:
{
  "valid": true,
  "userId": "用户ID"
}
```

### 聊天接口

```
POST /api/chat
Content-Type: application/json
Authorization: Bearer {token}

{
  "message": "用户消息",
  "model": "模型名称",
  "userId": "用户UUID"
}
```

## 前端实现

### 1. UUID管理 (auth.js)
- `generateUUID()`: 生成UUID v4
- `getDeviceUUID()`: 获取或创建设备UUID（存储在localStorage）

### 2. 认证功能 (auth.js)
- `login()`: 用户登录
- `register()`: 用户注册
- `logout()`: 用户登出
- `isAuthenticated()`: 检查是否已认证
- `getToken()`: 获取JWT令牌
- `getUserInfo()`: 获取用户信息

### 3. 请求拦截器 (request.js)
- 自动在请求头添加JWT令牌
- 自动在POST/PUT请求体中添加userId

## 配置说明

### application.yaml
```yaml
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.xurx.springai.entity
  configuration:
    map-underscore-to-camel-case: true
```

### JWT配置
在 `JwtUtils.java` 中配置：
- `SECRET_KEY`: 签名密钥（生产环境应从配置文件读取）
- `EXPIRATION_TIME`: 令牌有效期（默认7天）

## 使用流程

1. **首次访问**
   - 前端自动生成UUID并存储在localStorage
   - 调用登录接口获取JWT令牌
   - 后端自动创建用户记录

2. **发送消息**
   - 前端在请求中自动携带userId和JWT令牌
   - 后端验证令牌并保存聊天记录

3. **查询历史**
   - 可通过userId查询用户的所有聊天记录
   - 可通过sessionId查询特定会话的聊天记录

## 注意事项

1. **安全性**
   - JWT密钥应该从配置文件读取，不要硬编码
   - 生产环境应使用HTTPS
   - 考虑添加令牌刷新机制

2. **性能优化**
   - 考虑添加Redis缓存用户信息
   - 聊天记录可以分页查询
   - 可以添加索引优化查询性能

3. **扩展功能**
   - 可以添加用户头像上传
   - 可以添加会话管理功能
   - 可以添加聊天记录导出功能
