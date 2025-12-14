# Spring AI Frontend

基于 Vue 3 + Ant Design Vue 的 Spring AI 聊天界面

## 功能特性

- 左右布局,左侧可隐藏的对话列表
- 支持多对话管理(新建、切换、删除)
- Markdown 格式渲染(代码高亮)
- 本地存储对话历史
- 流式响应实时显示
- 简约现代的UI设计 (Ant Design)

## 安装依赖

```bash
cd frontend
npm install
```

## 开发环境运行

```bash
npm run dev
```

访问: http://localhost:5173

## 构建生产版本

```bash
npm run build
```

## 技术栈

- Vue 3 (Composition API)
- Ant Design Vue 4
- Vue Router 4
- Marked (Markdown 渲染)
- Highlight.js (代码高亮)
- Vite (构建工具)

## 注意事项

1. 确保后端服务已启动 (默认端口: 8080)
2. 前端会自动代理 `/chat` 请求到后端
3. 对话历史保存在浏览器 localStorage
4. 支持 Markdown 渲染和代码语法高亮
