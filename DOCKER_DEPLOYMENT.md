# SpringAI 项目 Docker 部署文档

## 目录
- [架构设计](#架构设计)
- [前置要求](#前置要求)
- [项目结构](#项目结构)
- [配置文件详解](#配置文件详解)
- [部署步骤](#部署步骤)
- [常见问题](#常见问题)

---

## 架构设计

### 容器架构图

```
                    ┌─────────────┐
                    │   Nginx     │
                    │  (前端+代理) │
                    │   :80       │
                    └──────┬──────┘
                           │
                    ┌──────▼──────┐
                    │ Spring Boot │
                    │   (后端)    │
                    │   :8080     │
                    └──┬────┬─────┘
                       │    │
              ┌────────┘    └────────┐
              ▼                      ▼
        ┌──────────┐           ┌──────────┐
        │  MySQL   │           │  Redis   │
        │  :3306   │           │  :6379   │
        └──────────┘           └──────────┘
```

### 容器说明

| 容器名 | 作用 | 端口 | 是否必需 |
|--------|------|------|----------|
| nginx | 前端静态文件 + 反向代理 | 80 | 是 |
| springai-backend | Spring Boot 应用 | 8080 | 是 |
| mysql | 数据库（用户、聊天记录） | 3306 | 是 |
| redis | 向量数据库 + 缓存 | 6379 | 是 |

---

## 前置要求

### 系统要求
- Docker 20.10+
- Docker Compose 2.0+
- 至少 4GB 内存
- 至少 10GB 磁盘空间

### 安装 Docker

#### Ubuntu/Debian

```bash
# 更新包索引
sudo apt-get update

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 安装 Docker Compose
sudo apt-get install docker-compose-plugin

# 验证安装
docker --version
docker compose version
```

#### CentOS/RHEL

```bash
# 安装 Docker
sudo yum install -y yum-utils
sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
sudo yum install docker-ce docker-ce-cli containerd.io docker-compose-plugin

# 启动 Docker
sudo systemctl start docker
sudo systemctl enable docker

# 验证安装
docker --version
docker compose version
```

#### macOS

```bash
# 使用 Homebrew 安装
brew install --cask docker

# 或下载 Docker Desktop
# https://www.docker.com/products/docker-desktop

# 验证安装
docker --version
docker compose version
```

#### Windows

```powershell
# 下载并安装 Docker Desktop
# https://www.docker.com/products/docker-desktop

# 验证安装（在 PowerShell 中）
docker --version
docker compose version
```

---

## 项目结构

```
springai/
├── docker-compose.yml              # 开发环境编排
├── docker-compose.prod.yml         # 生产环境编排
├── .env.docker                     # 环境变量（不提交到 Git）
├── .env.docker.example             # 环境变量示例
│
├── nginx/
│   ├── Dockerfile                  # Nginx 镜像构建文件
│   └── nginx.conf                  # Nginx 配置
│
├── springai/                       # 后端目录
│   ├── Dockerfile                  # 后端镜像构建文件
│   ├── pom.xml
│   └── src/
│
├── frontend/                       # 前端目录
│   ├── Dockerfile                  # 前端构建镜像
│   ├── package.json
│   └── src/
│
└── init-scripts/                   # 数据库初始化脚本
    └── init.sql
```

---

## 配置文件详解

### 1. 后端 Dockerfile

**文件路径**：`springai/Dockerfile`

```dockerfile
# 多阶段构建：减小镜像体积
# 阶段1：构建阶段
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 复制源代码并构建
COPY src ./src
RUN mvn clean package -DskipTests -B

# 阶段2：运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

# 创建非 root 用户（安全最佳实践）
RUN addgroup -g 1001 -S springai && \
    adduser -u 1001 -S springai -G springai

# 从构建阶段复制 jar 文件
COPY --from=builder /app/target/*.jar app.jar

# 修改文件所有者
RUN chown -R springai:springai /app

# 切换到非 root 用户
USER springai

# 暴露端口
EXPOSE 8080

# 健康检查（需要 Spring Boot Actuator 依赖）
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM 优化参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**关键点**：
- 多阶段构建减小镜像体积（从 ~800MB 降至 ~200MB）
- 使用非 root 用户运行（安全）
- 健康检查确保容器状态监控（需要添加 Spring Boot Actuator 依赖）
- JVM 参数优化内存使用

**注意**：健康检查需要在 `pom.xml` 中添加 Actuator 依赖：
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

如果不想添加 Actuator，可以使用简单的端口检查：
```dockerfile
HEALTHCHECK CMD curl -f http://localhost:8080 || exit 1
```

---

### 2. 前端 Dockerfile

**文件路径**：`frontend/Dockerfile`

```dockerfile
# 阶段1：构建前端资源
FROM node:18-alpine AS builder

WORKDIR /app

# 复制 package.json 并安装依赖（包括 devDependencies，因为构建需要）
COPY package*.json ./
RUN npm ci

# 复制源代码并构建
COPY . .
RUN npm run build

# 阶段2：运行阶段（只包含构建产物）
FROM nginx:alpine

# 删除默认的 nginx 页面
RUN rm -rf /usr/share/nginx/html/*

# 从构建阶段复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制自定义 nginx 配置（从 frontend 目录）
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 启动 nginx
CMD ["nginx", "-g", "daemon off;"]
```

**关键点**：
- 多阶段构建，最终镜像只包含静态文件
- 镜像体积 ~20MB（非常小）
- nginx.conf 需要放在 frontend 目录下

---

### 3. Nginx 配置

**文件路径**：`nginx/nginx.conf`

```nginx
server {
    listen 80;
    server_name localhost;

    # 前端静态资源
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://springai-backend:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # 聊天接口代理
    location /chat {
        proxy_pass http://springai-backend:8080/chat;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 流式响应支持
        proxy_buffering off;
        proxy_cache off;
    }

    # 认证接口代理
    location /auth {
        proxy_pass http://springai-backend:8080/auth;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # Gzip 压缩
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css text/xml text/javascript application/json application/javascript application/xml+rss;
}
```

**关键点**：
- `/api/` 路径重写，去掉前缀后转发到后端
- 流式响应禁用缓冲（重要！）
- Gzip 压缩节省带宽

---

### 4. docker-compose.yml（开发环境）

**文件路径**：`docker-compose.yml`

```yaml
version: '3.8'

services:
  # MySQL 数据库
  mysql:
    image: mysql:8.0
    container_name: springai-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD:-root123}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-springai}
      MYSQL_USER: ${MYSQL_USER:-springai}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD:-springai123}
      TZ: Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./init-scripts:/docker-entrypoint-initdb.d
    networks:
      - springai-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis
  redis:
    image: redis:7-alpine
    container_name: springai-redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD:-redis123}
    environment:
      TZ: Asia/Shanghai
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - springai-network
    healthcheck:
      test: ["CMD", "sh", "-c", "redis-cli -a $$REDIS_PASSWORD ping || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Spring Boot 后端
  springai-backend:
    build:
      context: ./springai
      dockerfile: Dockerfile
    container_name: springai-backend
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-prod}
      # 数据库配置
      DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE:-springai}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
      DATASOURCE_USERNAME: ${MYSQL_USER:-springai}
      DATASOURCE_PASSWORD: ${MYSQL_PASSWORD:-springai123}
      # Redis 配置
      DATA_REDIS_HOST: redis
      DATA_REDIS_PORT: 6379
      DATA_REDIS_PASSWORD: ${REDIS_PASSWORD:-redis123}
      # AI 模型配置
      MODELARGS_MODELS_DEEPSEEK_API-KEY: ${DEEPSEEK_API_KEY}
      MODELARGS_MODELS_DEEPSEEK_BASE-URL: ${DEEPSEEK_BASE_URL:-https://api.deepseek.com}
      MODELARGS_MODELS_DEEPSEEK_NAME: ${DEEPSEEK_MODEL:-deepseek-chat}
      MODELARGS_MODELS_QWEN_API-KEY: ${QWEN_API_KEY}
      MODELARGS_MODELS_QWEN_NAME: ${QWEN_MODEL:-qwen-turbo}
      MODELARGS_MODELS_ZHIPUAI-EMBED_API-KEY: ${ZHIPUAI_API_KEY}
      MODELARGS_MODELS_ZHIPUAI-EMBED_NAME: ${ZHIPUAI_EMBED_MODEL:-embedding-2}
      MODELARGS_MODELS_ZHIPUAI-EMBED_DIMENSIONS: ${ZHIPUAI_EMBED_DIMENSIONS:-1024}
      MODELARGS_MODELS_ZHIPUAI-EMBED_BASE-URL: ${ZHIPUAI_BASE_URL:-https://open.bigmodel.cn/api/paas/v4}
      TZ: Asia/Shanghai
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - springai-network
    volumes:
      - backend-logs:/app/logs

  # Nginx（前端 + 反向代理）
  nginx:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    container_name: springai-nginx
    restart: unless-stopped
    ports:
      - "80:80"
    depends_on:
      - springai-backend
    networks:
      - springai-network

networks:
  springai-network:
    driver: bridge

volumes:
  mysql-data:
    driver: local
  redis-data:
    driver: local
  backend-logs:
    driver: local
```

**关键点**：
- 使用环境变量配置敏感信息
- 服务依赖和健康检查
- 数据持久化（MySQL、Redis）
- 网络隔离

---

### 5. 生产环境配置

**文件路径**：`docker-compose.prod.yml`

```yaml
version: '3.8'

# 生产环境覆盖配置
services:
  mysql:
    command: --default-authentication-plugin=mysql_native_password
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --max_connections=200
    ports: []  # 生产环境不暴露端口

  redis:
    ports: []  # 生产环境不暴露端口

  springai-backend:
    environment:
      JAVA_OPTS: "-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
    ports: []  # 生产环境不暴露端口，只通过 Nginx 访问
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G

  nginx:
    ports:
      - "80:80"
      - "443:443"  # HTTPS
    volumes:
      - ./nginx/ssl:/etc/nginx/ssl:ro  # SSL 证书
```

**关键点**：
- 不对外暴露数据库端口
- 资源限制防止单个容器占用过多资源
- HTTPS 支持

---

### 6. 环境变量配置

**文件路径**：`.env.docker.example`

```bash
# ========== 数据库配置 ==========
MYSQL_ROOT_PASSWORD=your_root_password_here
MYSQL_DATABASE=springai
MYSQL_USER=springai
MYSQL_PASSWORD=your_mysql_password_here

# ========== Redis 配置 ==========
REDIS_PASSWORD=your_redis_password_here

# ========== Spring Boot 配置 ==========
SPRING_PROFILES_ACTIVE=prod

# ========== AI 模型配置 ==========
# DeepSeek
DEEPSEEK_API_KEY=your_deepseek_api_key_here
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat

# 阿里云通义千问
QWEN_API_KEY=your_qwen_api_key_here
QWEN_MODEL=qwen-turbo

# 智谱 AI（用于向量嵌入）
ZHIPUAI_API_KEY=your_zhipuai_api_key_here
ZHIPUAI_EMBED_MODEL=embedding-2
ZHIPUAI_EMBED_DIMENSIONS=1024
ZHIPUAI_BASE_URL=https://open.bigmodel.cn/api/paas/v4

# ========== 其他配置 ==========
TZ=Asia/Shanghai
```

**使用方法**：
```bash
# 1. 复制示例文件
cp .env.docker.example .env.docker

# 2. 编辑 .env.docker 填入真实配置
vim .env.docker

# 3. 确保 .env.docker 在 .gitignore 中
echo ".env.docker" >> .gitignore
```

---

## 部署步骤

### 步骤 1：准备服务器

```bash
# 1. 登录服务器
ssh user@your-server-ip

# 2. 更新系统
sudo apt-get update && sudo apt-get upgrade -y

# 3. 安装 Docker 和 Docker Compose
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo apt-get install docker-compose-plugin

# 4. 验证安装
docker --version
docker compose version
```

---

### 步骤 2：上传项目代码

```bash
# 方式 1：使用 Git（推荐）
git clone https://github.com/your-username/springai.git
cd springai

# 方式 2：使用 scp 上传
# 在本地执行
scp -r /path/to/springai user@your-server-ip:/home/user/

# 方式 3：使用 rsync
rsync -avz --exclude 'node_modules' --exclude 'target' \
  /path/to/springai user@your-server-ip:/home/user/
```

---

### 步骤 3：配置环境变量

```bash
# 1. 复制环境变量模板
cp .env.docker.example .env.docker

# 2. 编辑配置文件
vim .env.docker

# 3. 填入真实的 API Key 和密码
# 重点配置：
# - MYSQL_ROOT_PASSWORD（MySQL root 密码）
# - MYSQL_PASSWORD（应用数据库密码）
# - REDIS_PASSWORD（Redis 密码）
# - DEEPSEEK_API_KEY（DeepSeek API Key）
# - QWEN_API_KEY（通义千问 API Key）
# - ZHIPUAI_API_KEY（智谱 AI API Key）
```

---

### 步骤 4：准备数据库初始化脚本

**文件路径**：`init-scripts/init.sql`

```sql
-- 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 创建聊天记录表
CREATE TABLE IF NOT EXISTS chat_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    session_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    model VARCHAR(100),
    metadata JSON,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 步骤 5：构建和启动容器

#### 开发环境部署

```bash
# 1. 构建镜像
docker compose build

# 2. 启动所有服务
docker compose up -d

# 3. 查看日志
docker compose logs -f

# 4. 查看容器状态
docker compose ps
```

#### 生产环境部署

```bash
# 1. 使用生产配置构建
docker compose -f docker-compose.yml -f docker-compose.prod.yml build

# 2. 启动服务
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d

# 3. 查看日志
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f
```

---

### 步骤 6：验证部署

```bash
# 1. 检查容器状态
docker compose ps

# 预期输出：
# NAME                 STATUS              PORTS
# springai-mysql       Up (healthy)        0.0.0.0:3306->3306/tcp
# springai-redis       Up (healthy)        0.0.0.0:6379->6379/tcp
# springai-backend     Up (healthy)        0.0.0.0:8080->8080/tcp
# springai-nginx       Up                  0.0.0.0:80->80/tcp

# 2. 测试后端健康检查
curl http://localhost:8080/actuator/health

# 预期输出：
# {"status":"UP"}

# 3. 测试前端访问
curl http://localhost

# 4. 在浏览器访问
# http://your-server-ip
```

---

### 步骤 7：配置域名（可选）

```bash
# 1. 配置 DNS 解析
# 在你的域名提供商添加 A 记录：
# springai.yourdomain.com -> your-server-ip

# 2. 修改 nginx.conf
vim nginx/nginx.conf

# 将 server_name 修改为你的域名：
# server_name springai.yourdomain.com;

# 3. 重启 Nginx 容器
docker compose restart nginx

# 4. 配置 HTTPS（使用 Let's Encrypt）
# 安装 certbot
sudo apt-get install certbot python3-certbot-nginx

# 生成证书
sudo certbot --nginx -d springai.yourdomain.com

# 自动续期
sudo certbot renew --dry-run
```

---

## 常用操作命令

### 容器管理

```bash
# 启动所有服务
docker compose up -d

# 停止所有服务
docker compose down

# 重启特定服务
docker compose restart springai-backend

# 查看日志
docker compose logs -f springai-backend

# 查看实时日志（所有服务）
docker compose logs -f

# 进入容器
docker compose exec springai-backend sh

# 查看容器资源使用
docker stats
```

### 数据管理

```bash
# 备份 MySQL 数据
docker compose exec mysql mysqldump -u root -p springai > backup.sql

# 恢复 MySQL 数据
docker compose exec -T mysql mysql -u root -p springai < backup.sql

# 清理未使用的数据卷
docker volume prune

# 查看数据卷
docker volume ls
```

### 镜像管理

```bash
# 重新构建镜像
docker compose build --no-cache

# 拉取最新镜像
docker compose pull

# 清理未使用的镜像
docker image prune -a

# 查看镜像大小
docker images
```

---

## 常见问题

### 1. 容器启动失败

**问题**：容器启动后立即退出

**解决方法**：
```bash
# 查看容器日志
docker compose logs springai-backend

# 常见原因：
# - 环境变量配置错误
# - 依赖服务未就绪
# - 端口被占用

# 检查端口占用
sudo netstat -tlnp | grep :8080
```

### 2. 数据库连接失败

**问题**：后端无法连接 MySQL

**解决方法**：
```bash
# 1. 检查 MySQL 是否正常运行
docker compose exec mysql mysqladmin -u root -p ping

# 2. 检查网络连接
docker compose exec springai-backend ping mysql

# 3. 检查环境变量
docker compose exec springai-backend env | grep DATASOURCE

# 4. 手动测试连接
docker compose exec mysql mysql -u springai -p
```

### 3. 前端无法访问后端

**问题**：前端页面打开但无法调用 API

**解决方法**：
```bash
# 1. 检查 Nginx 配置
docker compose exec nginx cat /etc/nginx/conf.d/default.conf

# 2. 检查网络连接
docker compose exec nginx ping springai-backend

# 3. 查看 Nginx 日志
docker compose logs nginx

# 4. 测试反向代理
curl http://localhost/api/actuator/health
```

### 4. Redis 连接失败

**问题**：无法连接 Redis

**解决方法**：
```bash
# 1. 检查 Redis 是否运行
docker compose exec redis redis-cli ping

# 2. 使用密码连接
docker compose exec redis redis-cli -a your_password ping

# 3. 检查配置
docker compose exec springai-backend env | grep REDIS
```

### 5. 内存不足

**问题**：容器频繁重启或 OOM

**解决方法**：
```bash
# 1. 查看资源使用
docker stats

# 2. 调整 JVM 内存
# 编辑 docker-compose.yml
# JAVA_OPTS: "-Xms512m -Xmx1024m"

# 3. 限制容器资源
# 在 docker-compose.prod.yml 中设置资源限制
```

### 6. 构建镜像速度慢

**问题**：构建镜像时下载依赖慢

**解决方法**：
```bash
# 使用国内镜像源
# 在后端 Dockerfile 中添加：
RUN mvn dependency:go-offline -B \
    -DmultiThreaded=true \
    -Dmaven.repo.local=/root/.m2/repository

# 前端使用淘宝镜像：
RUN npm config set registry https://registry.npmmirror.com && \
    npm ci --only=production
```

---

## 监控和维护

### 日志管理

```bash
# 限制日志大小（在 docker-compose.yml 中）
services:
  springai-backend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

### 定期备份

```bash
# 创建备份脚本 backup.sh
#!/bin/bash
DATE=$(date +%Y%m%d_%H%M%S)
docker compose exec mysql mysqldump -u root -p${MYSQL_ROOT_PASSWORD} --all-databases > backup_${DATE}.sql
```

### 自动更新

```bash
# 创建更新脚本 update.sh
#!/bin/bash
git pull
docker compose build
docker compose up -d
docker compose logs -f
```

---

## 性能优化建议

### 1. 数据库优化

```sql
-- 添加索引
ALTER TABLE chat_records ADD INDEX idx_user_session (user_id, session_id);

-- 定期清理旧数据
DELETE FROM chat_records WHERE create_time < DATE_SUB(NOW(), INTERVAL 30 DAY);
```

### 2. Redis 优化

```bash
# 启用持久化
redis-server --appendonly yes

# 设置最大内存
redis-server --maxmemory 512mb --maxmemory-policy allkeys-lru
```

### 3. Nginx 优化

```nginx
# 开启 HTTP/2
listen 443 ssl http2;

# 启用缓存
proxy_cache_path /var/cache/nginx levels=1:2 keys_zone=my_cache:10m;
```

---

## 安全建议

1. **不要使用默认密码**：修改所有默认密码
2. **使用 HTTPS**：配置 SSL 证书
3. **限制端口暴露**：生产环境只暴露 Nginx 端口
4. **定期更新镜像**：及时更新基础镜像和依赖
5. **使用 Docker secrets**：敏感信息使用 secrets 管理
6. **配置防火墙**：限制访问来源

```bash
# 配置防火墙
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
```

---

## 总结

### 优势
- ✅ 一键部署，环境一致
- ✅ 服务隔离，易于维护
- ✅ 可横向扩展
- ✅ 易于回滚和更新

### 部署检查清单

- [ ] 服务器配置完成（Docker + Docker Compose）
- [ ] 代码上传到服务器
- [ ] 环境变量配置完成（.env.docker）
- [ ] 数据库初始化脚本准备完成
- [ ] 所有容器成功启动
- [ ] 健康检查通过
- [ ] 前端页面可访问
- [ ] API 接口测试通过
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] 日志输出正常
- [ ] 域名解析配置（如需要）
- [ ] HTTPS 证书配置（如需要）
- [ ] 备份策略制定
- [ ] 监控告警配置

---

## 联系方式

如有问题，请提交 Issue 或联系维护团队。
