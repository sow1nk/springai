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

# 健康检查（使用简单的端口检查）
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -s -o /dev/null http://localhost:8080 || exit 1

# JVM 优化参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Djava.security.egd=file:/dev/./urandom"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]