# 多阶段构建：先打包再运行，减少最终镜像体积
FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 仅复制 pom.xml 以利用缓存预下载依赖
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 复制源码并构建可执行 jar
COPY src ./src
RUN mvn -B clean package -DskipTests

# 运行阶段使用精简 JRE 镜像
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 安装基础工具，用于运行和健康检查
RUN apk add --no-cache curl

# 创建非 root 用户，提升安全性
RUN addgroup -g 1001 -S springai && \
    adduser -u 1001 -S springai -G springai

# 拷贝构建产物
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R springai:springai /app
USER springai

# 设置默认 JVM 参数，可通过 docker run -e JAVA_OPTS="..." 覆盖
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

# MCP Server 默认监听 8081
EXPOSE 8081

# 简单健康检查：端口可用即视为健康
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD curl --fail --silent http://localhost:8081/actuator/health || curl --fail --silent http://localhost:8081 || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
