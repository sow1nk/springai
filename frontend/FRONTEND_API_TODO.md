# Frontend API TODO & Optimization Notes

## 未使用的 API / Helper

| 位置 | 导出 | 当前状态 | TODO |
| --- | --- | --- | --- |
| `src/api/chat.js:59` | `getChatHistoryBySession` | 仅后端提供 session 级查询，前端没有入口使用 | 评估是否需要“按会话进入详情”的功能；若无需求可以删除接口，减少 bundle 体积 |
| `src/api/chat.js:71` | `getUserConversations` | 会话列表目前由 `getChatHistory` 拉取后在前端分组 | 若要支持快速概览或分页，改为调用该接口；没需求则删除 |
| `src/api/chat.js:81` | `getConversationMessages` | 没有会话详情页使用它 | 设计会话详情/分享页时接入，否则删除 |
| `src/api/request.js:99` | `sendChatMessage` | 仅提供非流式发送，现有 UI 全部使用 `sendChatMessageStream` | 如果后续需要“非流式/一次性响应”入口则在对应组件中引用；否则删除 |
| `src/api/request.js:208` | `export default request` | axios 实例未被任何组件直接使用 | 要么改造所有 API 统一通过该实例，要么删掉避免重复拦截器 |

## 可优化项 TODO

1. **历史记录拆分逻辑**：`src/views/ChatView.vue` 目前调用 `getChatHistory` 拉取所有记录再在前端以 `sessionId` 分组，存在重复排序/格式化逻辑。可以改为：
   - 后端直接返回按 session 聚合的结果；或
   - 使用 `getUserConversations` + `getConversationMessages` 分步拉取，减少一次性数据量。

2. **请求封装统一化**：`src/api/chat.js`、`src/api/auth.js` 和 `src/api/request.js` 各自创建/修改 axios 配置（甚至两套拦截器）。建议统一使用 `request.js` 中的 axios 实例，并删除重复的全局 `axios.interceptors`，避免拦截器叠加或难以维护。

3. **会话 ID 处理**：`loadChatHistoryFromDB` 中通过 `Map` 二次处理 session 标题/时间。如果未来要使用 `getChatHistoryBySession` 等接口，可将这段逻辑抽成 util，避免在多个视图重复实现。

4. **错误提示一致性**：`src/api/request.js` 使用 `antMessage`，`src/api/chat.js` 则直接 `throw new Error` 并在视图捕获。可以约定统一的异常格式，方便在视图层展示一致的提示。

根据以上 TODO，优先决定哪些接口会被真正使用，再清理或重构，避免留存无用代码影响可读性。

