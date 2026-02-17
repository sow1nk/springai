# Spring AI Chat

基于 Spring AI 的多模型智能对话平台，集成意图识别、RAG 知识库检索、MCP 工具调用与工作流编排。

## 架构概览

```
┌──────────────┐      ┌──────────────────┐      ┌─────────────────┐
│   Frontend   │─────▶│  Spring Boot AI  │─────▶│   MCP Server    │
│  Vue 3 + Ant │      │ (Spring AI 1.1)  │      │ Math/Date/SQL   │
└──────────────┘      └────────┬─────────┘      └─────────────────┘
   via Nginx                   │
                     ┌─────────┴───────┐
                     │                 │
                ┌────▼────┐      ┌─────▼───────┐
                │  MySQL  │      │ Redis Stack │
                │   8.0   │      │  Vector DB  │
                └─────────┘      └─────────────┘
```

## 核心功能

- **多模型切换** — DeepSeek / 通义千问 / 智谱 GLM，前端一键选择
- **意图识别 + 工作流编排** — 基于 Spring AI Alibaba StateGraph，自动将用户输入路由至闲聊、RAG 或工具调用
- **RAG 知识库** — 智谱 Embedding + Redis 向量存储，支持 CSV 数据导入与相似度检索
- **MCP 工具调用** — 通过 Model Context Protocol 调用外部工具（数学运算、日期计算、数据库查询）
- **流式响应** — SSE 实时推送工作流步骤与 Token，前端逐字渲染
- **用户认证** — JWT + BCrypt，支持注册/登录/会话管理
- **对话持久化** — MySQL 存储聊天记录，JDBC Chat Memory 保持上下文

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5 / Java 17 |
| AI 框架 | Spring AI 1.1 / Spring AI Alibaba 1.1-RC2 |
| 大模型 | DeepSeek Chat / Qwen (DashScope) / ZhipuAI GLM-4.5 |
| 向量存储 | Redis Stack (RediSearch) |
| 数据库 | MySQL 8.0 / MyBatis |
| 认证 | JWT (JJWT 0.12) / Spring Security Crypto |
| 前端 | Vue 3 / Ant Design Vue 4 / Vite 5 |
| 流式通信 | SSE (fetch-event-source) |
| 部署 | Docker Compose / Nginx |

## 项目结构

```
├── springai/                  # 主后端服务
│   └── src/main/java/.../
│       ├── controller/        # REST API (Chat, Auth, RAG, Menu, Graph)
│       ├── service/           # 业务逻辑 (IntentGraph, ChatRecord, User)
│       ├── configuration/     # ChatClient / 模型参数配置
│       ├── advisor/           # Token 统计 / 敏感词过滤 / ReReading
│       ├── entity/ mapper/    # 数据层
│       └── utils/             # JWT / 密码工具
├── springai-mcp-server/       # MCP 工具服务 (数学/日期/MySQL)
├── frontend/                  # Vue 3 前端
│   └── src/
│       ├── components/        # Chat / Markdown / Sidebar
│       └── views/             # Auth / Chat 页面
├── docker-compose.yml         # 开发环境编排
└── docker-compose.prod.yml    # 生产环境覆盖
```

## 快速开始

### 环境要求

- Java 17+
- Node.js 18+
- Docker & Docker Compose

### Docker 一键部署

1. 创建 `.env` 文件配置 API 密钥：

```bash
DEEPSEEK_API_KEY=your_key
QWEN_API_KEY=your_key
ZHIPUAI_API_KEY=your_key
```

2. 启动所有服务：

```bash
docker compose up -d
```

3. 访问 `http://localhost:8085`

### 本地开发

**后端：**

```bash
cd springai
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**MCP Server：**

```bash
cd springai-mcp-server
mvn spring-boot:run
```

**前端：**

```bash
cd frontend
npm install
npm run dev
```

## API 端点

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/chat` | 流式对话（SSE） |
| GET | `/chat/history` | 获取用户会话列表 |
| GET | `/chat/history/{sessionId}` | 获取会话消息 |
| DELETE | `/chat/conversations/{sessionId}` | 删除会话 |
| POST | `/auth/login` | 用户登录 |
| POST | `/auth/register` | 用户注册 |
| POST | `/menu/importData` | 导入知识库数据 |
| POST | `/menu/search` | 知识库检索 |

## 服务端口

| 服务 | 端口 |
|------|------|
| Nginx (前端) | 8085 |
| Spring Boot 后端 | 8080 |
| MCP Server | 8081 |
| MySQL | 3307 |
| Redis Stack | 6380 |
