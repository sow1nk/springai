# Docker Compose 编写规范与最佳实践

## 目录
- [基础结构](#基础结构)
- [版本说明](#版本说明)
- [核心配置项](#核心配置项)
- [最佳实践](#最佳实践)
- [完整示例](#完整示例)

---

## 基础结构

### 最简单的 docker-compose.yml

```yaml
version: '3.8'

services:
  web:
    image: nginx:alpine
    ports:
      - "80:80"
```

### 标准结构

```yaml
version: '3.8'                    # Compose 文件版本

services:                         # 服务定义
  service1:                       # 服务名称
    # 服务配置...
  service2:
    # 服务配置...

networks:                         # 网络定义（可选）
  network1:
    # 网络配置...

volumes:                          # 数据卷定义（可选）
  volume1:
    # 数据卷配置...

configs:                          # 配置文件（可选，v3.3+）
  config1:
    # 配置...

secrets:                          # 敏感信息（可选，v3.1+）
  secret1:
    # 密钥配置...
```

---

## 版本说明

### Compose 文件版本对照表

| 版本 | Docker Engine | 发布时间 | 主要特性 |
|------|---------------|----------|----------|
| 3.8 | 19.03.0+ | 2019 | 推荐使用，支持大部分特性 |
| 3.7 | 18.06.0+ | 2018 | 支持 init 选项 |
| 3.6 | 18.02.0+ | 2018 | 支持 tmpfs size |
| 3.5 | 17.12.0+ | 2017 | 支持隔离模式 |
| 3.4 | 17.09.0+ | 2017 | 支持 target 和 network 配置 |
| 3.3 | 17.06.0+ | 2017 | 支持 configs |
| 3.0 | 1.13.0+ | 2017 | 支持 Swarm mode |

**推荐使用 3.8 版本**（兼容性好，功能完善）

```yaml
version: '3.8'  # 推荐
```

---

## 核心配置项详解

### 1. services（服务配置）

这是 docker-compose 的核心部分，定义所有容器服务。

#### 1.1 image - 指定镜像

```yaml
services:
  web:
    # 方式1：使用官方镜像
    image: nginx:alpine

  app:
    # 方式2：使用私有仓库镜像
    image: registry.example.com/myapp:1.0.0

  db:
    # 方式3：使用 Docker Hub 用户镜像
    image: mysql:8.0
```

#### 1.2 build - 构建镜像

```yaml
services:
  web:
    # 方式1：简单指定构建目录
    build: ./dir

  app:
    # 方式2：详细配置
    build:
      context: ./dir              # 构建上下文目录
      dockerfile: Dockerfile.dev  # 指定 Dockerfile
      args:                       # 构建参数
        VERSION: "1.0"
        ENV: "production"
      target: production          # 多阶段构建的目标阶段
      cache_from:                 # 缓存来源
        - alpine:latest
      labels:                     # 镜像标签
        com.example.version: "1.0"
```

#### 1.3 container_name - 容器名称

```yaml
services:
  web:
    container_name: my-web-container  # 自定义容器名
    image: nginx:alpine

  # 注意：如果不指定，Docker Compose 会自动生成名称
  # 格式：<项目名>_<服务名>_<序号>
  # 例如：myproject_web_1
```

#### 1.4 ports - 端口映射

```yaml
services:
  web:
    image: nginx:alpine
    ports:
      # 格式1：HOST:CONTAINER
      - "80:80"
      - "443:443"

      # 格式2：指定协议
      - "8080:8080/tcp"
      - "53:53/udp"

      # 格式3：指定 IP
      - "127.0.0.1:8080:8080"

      # 格式4：只指定容器端口（主机端口随机）
      - "8080"

      # 格式5：长格式（推荐用于复杂配置）
      - target: 80        # 容器端口
        published: 8080   # 主机端口
        protocol: tcp     # 协议
        mode: host        # 模式：host 或 ingress
```

**最佳实践**：
```yaml
# ✅ 推荐：使用字符串格式，避免 YAML 解析问题
ports:
  - "80:80"
  - "443:443"

# ❌ 不推荐：数字格式可能被解析为八进制
ports:
  - 80:80  # 可能出问题
```

#### 1.5 expose - 暴露端口（仅容器间通信）

```yaml
services:
  web:
    image: nginx:alpine
    expose:
      - "80"      # 只在内部网络暴露，不映射到主机
      - "443"
```

**区别**：
- `ports`: 映射到主机，外部可访问
- `expose`: 仅容器间可访问，不映射到主机

#### 1.6 environment - 环境变量

```yaml
services:
  web:
    image: nginx:alpine
    environment:
      # 方式1：键值对格式
      NODE_ENV: production
      DEBUG: "false"
      PORT: 3000

  app:
    image: myapp:latest
    environment:
      # 方式2：数组格式
      - NODE_ENV=production
      - DEBUG=false
      - API_KEY=${API_KEY}  # 从宿主机环境变量读取

  db:
    image: mysql:8.0
    environment:
      # 方式3：使用 .env 文件
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
```

**最佳实践**：
```yaml
# ✅ 推荐：敏感信息使用环境变量
environment:
  DB_PASSWORD: ${DB_PASSWORD}

# ❌ 不推荐：敏感信息硬编码
environment:
  DB_PASSWORD: "mypassword123"
```

#### 1.7 env_file - 环境变量文件

```yaml
services:
  web:
    image: nginx:alpine
    # 方式1：单个文件
    env_file: .env

  app:
    image: myapp:latest
    # 方式2：多个文件（后面的会覆盖前面的）
    env_file:
      - .env.common
      - .env.production
```

**.env 文件示例**：
```bash
# .env
NODE_ENV=production
PORT=3000
API_KEY=your_api_key_here
```

#### 1.8 volumes - 数据卷挂载

```yaml
services:
  web:
    image: nginx:alpine
    volumes:
      # 格式1：命名卷
      - data-volume:/var/lib/data

      # 格式2：绑定挂载（主机路径:容器路径）
      - ./nginx.conf:/etc/nginx/nginx.conf

      # 格式3：只读挂载
      - ./config:/etc/config:ro

      # 格式4：长格式（推荐）
      - type: bind
        source: ./app
        target: /usr/share/nginx/html
        read_only: true

      # 格式5：匿名卷
      - /var/log

volumes:
  data-volume:  # 定义命名卷
    driver: local
```

**路径说明**：
- 相对路径：相对于 docker-compose.yml 文件位置
- 绝对路径：从根目录开始
- 命名卷：需要在顶层 volumes 中定义

#### 1.9 networks - 网络配置

```yaml
services:
  web:
    image: nginx:alpine
    networks:
      - frontend
      - backend

  app:
    image: myapp:latest
    networks:
      backend:
        # 指定 IP 地址
        ipv4_address: 172.16.238.10

  db:
    image: mysql:8.0
    networks:
      - backend

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    ipam:
      config:
        - subnet: 172.16.238.0/24
```

**网络模式**：
```yaml
services:
  web:
    # 使用主机网络
    network_mode: "host"

  app:
    # 使用其他容器的网络
    network_mode: "service:web"

  db:
    # 使用桥接网络（默认）
    network_mode: "bridge"
```

#### 1.10 depends_on - 服务依赖

```yaml
services:
  web:
    image: nginx:alpine
    depends_on:
      - app
      - db

  app:
    image: myapp:latest
    depends_on:
      db:
        condition: service_healthy  # 等待健康检查通过

  db:
    image: mysql:8.0
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
```

**注意**：`depends_on` 只控制启动顺序，不等待服务完全就绪。

#### 1.11 restart - 重启策略

```yaml
services:
  web:
    image: nginx:alpine
    restart: always  # 总是重启

  app:
    image: myapp:latest
    restart: unless-stopped  # 除非手动停止，否则总是重启（推荐）

  db:
    image: mysql:8.0
    restart: on-failure  # 只在失败时重启

  cache:
    image: redis:alpine
    restart: "no"  # 不自动重启（默认）
```

**重启策略说明**：
- `no`: 不自动重启（默认）
- `always`: 总是重启
- `on-failure`: 只在容器退出码非 0 时重启
- `unless-stopped`: 除非手动停止，否则总是重启（推荐生产环境使用）

#### 1.12 command - 覆盖默认命令

```yaml
services:
  web:
    image: nginx:alpine
    # 方式1：字符串格式
    command: nginx -g 'daemon off;'

  app:
    image: myapp:latest
    # 方式2：数组格式（推荐）
    command: ["npm", "start"]

  db:
    image: mysql:8.0
    # 方式3：多行命令
    command: >
      --default-authentication-plugin=mysql_native_password
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
```

#### 1.13 entrypoint - 覆盖入口点

```yaml
services:
  web:
    image: nginx:alpine
    entrypoint: /bin/sh

  app:
    image: myapp:latest
    entrypoint: ["python", "app.py"]
```

**command vs entrypoint**：
- `entrypoint`: 定义容器启动时执行的命令
- `command`: 提供给 entrypoint 的参数

#### 1.14 healthcheck - 健康检查

```yaml
services:
  web:
    image: nginx:alpine
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
```

---

## 最佳实践

### 1. 使用 .env 文件管理环境变量

```yaml
# docker-compose.yml
version: '3.8'

services:
  db:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE}
```

```bash
# .env
MYSQL_ROOT_PASSWORD=secure_password
MYSQL_DATABASE=myapp
```

### 2. 使用命名卷而非绑定挂载

```yaml
# ✅ 推荐：命名卷
volumes:
  - mysql-data:/var/lib/mysql

volumes:
  mysql-data:

# ❌ 不推荐：绑定挂载（除非需要编辑文件）
volumes:
  - ./data:/var/lib/mysql
```

### 3. 使用健康检查和依赖条件

```yaml
services:
  app:
    depends_on:
      db:
        condition: service_healthy

  db:
    healthcheck:
      test: ["CMD", "mysqladmin", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### 4. 限制日志大小

```yaml
services:
  web:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

---

## 完整示例：SpringAI 项目

### 针对你的 SpringAI 项目的标准 docker-compose.yml

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
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  # Redis 缓存和向量数据库
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
      test: ["CMD", "redis-cli", "ping"]
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
      DATASOURCE_URL: jdbc:mysql://mysql:3306/${MYSQL_DATABASE:-springai}
      DATASOURCE_USERNAME: ${MYSQL_USER:-springai}
      DATASOURCE_PASSWORD: ${MYSQL_PASSWORD:-springai123}
      DATA_REDIS_HOST: redis
      DATA_REDIS_PORT: 6379
      DATA_REDIS_PASSWORD: ${REDIS_PASSWORD:-redis123}
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

### 关键配置说明

1. **环境变量默认值**：`${VAR:-default}` 语法
2. **健康检查**：确保服务就绪后再启动依赖服务
3. **网络隔离**：所有服务在同一网络中通信
4. **数据持久化**：使用命名卷保存数据
5. **日志管理**：限制日志文件大小

### 使用方法

```bash
# 1. 创建 .env 文件
cp .env.docker.example .env.docker

# 2. 启动所有服务
docker compose up -d

# 3. 查看日志
docker compose logs -f

# 4. 停止服务
docker compose down
```

---

## 总结

### Docker Compose 核心要点

1. **版本选择**：推荐使用 3.8
2. **服务配置**：image/build、ports、volumes、environment
3. **依赖管理**：depends_on + healthcheck
4. **网络隔离**：使用自定义网络
5. **数据持久化**：使用命名卷
6. **环境变量**：使用 .env 文件管理
7. **日志管理**：限制日志大小
8. **重启策略**：生产环境使用 unless-stopped

### 最佳实践检查清单

- [ ] 使用 .env 文件管理敏感信息
- [ ] 配置健康检查
- [ ] 使用命名卷而非绑定挂载
- [ ] 限制日志文件大小
- [ ] 设置合理的重启策略
- [ ] 使用自定义网络隔离服务
- [ ] 生产环境不暴露数据库端口
- [ ] 添加容器资源限制（生产环境）
```

