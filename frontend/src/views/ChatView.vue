<template>
  <div class="chat-view">
    <Sidebar
      :collapsed="sidebarCollapsed"
      :chats="chats"
      :active-id="activeChat"
      @toggle="toggleSidebar"
      @new-chat="createNewChat"
      @select-chat="selectChat"
      @delete-chat="deleteChat"
    />

    <div class="chat-main">
      <div class="chat-header">
        <div class="header-left">
          <h1>Spring AI 助手</h1>
          <p>支持数学计算和MySQL数据库查询</p>
        </div>
        <div class="header-right">
          <a-dropdown>
            <a-avatar
              :style="{ backgroundColor: 'var(--accent-color)', cursor: 'pointer' }"
              aria-label="用户菜单"
            >
              {{ userName }}
            </a-avatar>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" aria-label="个人信息">
                  <UserOutlined />
                  个人信息
                </a-menu-item>
                <a-menu-item key="settings" aria-label="设置">
                  <SettingOutlined />
                  设置
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" aria-label="退出登录">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </div>

      <div class="chat-content-wrapper" ref="messagesContainer">
        <div class="chat-content-column">
          <div class="chat-messages">
            <div
              v-for="(message, index) in currentMessages"
              :key="index"
              :class="['message-card', message.role]"
            >
              <div class="message-meta">
                <div class="meta-left">
                  <a-avatar
                    v-if="message.role === 'user'"
                    :style="{
                      backgroundColor: 'var(--accent-color)',
                      flexShrink: 0
                    }"
                  >
                    {{ userName }}
                  </a-avatar>
                  <a-avatar
                    v-else
                    :src="AIAvatar"
                    :style="{ flexShrink: 0 }"
                  ></a-avatar>
                  <div class="meta-info">
                    <span class="meta-name">{{ message.role === 'user' ? userName : 'Spring AI' }}</span>
                    <span class="meta-time">{{ message.time }}</span>
                  </div>
                </div>
                <div class="meta-actions">
                  <button class="meta-btn" @click="copyMessage(message.content)">复制</button>
                </div>
              </div>
              <div class="message-content">
                <MarkdownRenderer
                  v-if="message.role === 'assistant'"
                  :content="message.content"
                />
                <div v-else class="message-text">{{ message.content }}</div>
              </div>
            </div>

            <div v-if="streamingMessage" class="message-card assistant">
              <div class="message-meta">
                <div class="meta-left">
                  <a-avatar :src="AIAvatar" :style="{ flexShrink: 0 }"></a-avatar>
                  <div class="meta-info">
                    <span class="meta-name">Spring AI</span>
                    <span class="meta-time">实时生成</span>
                  </div>
                </div>
              </div>
              <div class="message-content">
                <MarkdownRenderer :content="streamingMessage" />
              </div>
            </div>

            <div v-if="loading && !streamingMessage" class="message-card assistant">
              <div class="message-meta">
                <div class="meta-left">
                  <a-avatar :src="AIAvatar" :style="{ flexShrink: 0 }"></a-avatar>
                  <div class="meta-info">
                    <span class="meta-name">Spring AI</span>
                    <span class="meta-time">思考中...</span>
                  </div>
                </div>
              </div>
              <div class="message-content">
                <a-spin size="small" />
              </div>
            </div>

            <div v-if="currentMessages.length === 0 && !loading" class="empty-state">
              <div class="empty-icon">
                <img :src="panda" alt="Panda" />
              </div>
              <h2>开始新对话</h2>
              <p>向我提问数学问题或数据库查询</p>
              <div class="examples">
                <div
                  v-for="(example, index) in examples"
                  :key="index"
                  @click="useExample(example)"
                  class="example-card"
                >
                  {{ example }}
                </div>
              </div>
            </div>
          </div>

          <div class="chat-input-area">
            <div class="chat-input">
              <a-textarea
                v-model:value="inputMessage"
                :rows="1"
                :auto-size="{ minRows: 1, maxRows: 6 }"
                placeholder="Message Spring AI"
                @keydown.enter="handleKeydown"
                :disabled="loading"
                class="message-input"
              />
              <div class="input-actions">
                <a-button type="text" class="input-icon" aria-label="插入示例" @click="useExample(examples[0])">
                  <template #icon>
                    <PlusOutlined />
                  </template>
                </a-button>
                <button
                  class="send-button"
                  @click="sendMessage"
                  :disabled="!inputMessage.trim() || loading"
                  aria-label="发送消息"
                >
                  <ArrowUpOutlined />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ArrowUpOutlined, UserOutlined, SettingOutlined, LogoutOutlined, ExclamationCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import Sidebar from '../components/Sidebar.vue'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'

const sidebarCollapsed = ref(false)
const chats = ref([])
const activeChat = ref(null)
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)
const streamingMessage = ref('')
const userName = ref('我')
const abortController = ref(null)
const panda = ref("/images/panda.svg")
const AIAvatar = ref("/images/xurx_masaike.png")

const examples = [
  '计算 156 + 789',
  '125 乘以 48 等于多少',
  '查询数据库中有哪些表',
  '1000 除以 25',
  '用markdown格式写一个代码示例'
]

const normalizeContent = (content) => {
  if (typeof content === 'string') {
    return content
  }
  try {
    return JSON.stringify(content, null, 2)
  } catch (error) {
    console.error('文本格式化失败:', error)
    return String(content)
  }
}

const currentMessages = computed(() => {
  if (!activeChat.value) return []
  const chat = chats.value.find(c => c.id === activeChat.value)
  return chat ? chat.messages : []
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const generateId = () => {
  return Date.now().toString(36) + Math.random().toString(36).substring(2)
}

const getCurrentTime = () => {
  const now = new Date()
  return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const createNewChat = () => {
  const newChat = {
    id: generateId(),
    title: `对话 ${chats.value.length + 1}`,
    messages: [],
    createdAt: new Date()
  }
  chats.value.unshift(newChat)
  activeChat.value = newChat.id
  saveChats()
}

const selectChat = (id) => {
  activeChat.value = id
}

const deleteChat = (id) => {
  // 防止删除不存在的对话
  const chat = chats.value.find(c => c.id === id)
  if (!chat) {
    console.error('对话不存在:', id)
    return
  }

  const chatTitle = chat.title

  Modal.confirm({
    title: '确认删除',
    content: `确定要删除 "${chatTitle}" 吗？此操作无法撤销。`,
    icon: h(ExclamationCircleOutlined),
    okText: '删除',
    okType: 'danger',
    cancelText: '取消',
    centered: true,
    onOk() {
      try {
        const index = chats.value.findIndex(c => c.id === id)

        if (index === -1) {
          console.error('无法找到要删除的对话')
          return
        }

        // 记录删除前的状态
        const wasActiveChat = activeChat.value === id
        const remainingChats = chats.value.length - 1

        // 删除对话
        chats.value.splice(index, 1)

        // 处理删除后的活动对话切换
        if (wasActiveChat) {
          if (remainingChats > 0) {
            // 优先选择下一个对话，如果没有则选择第一个
            activeChat.value = chats.value[Math.min(index, chats.value.length - 1)].id
          } else {
            // 没有对话了，先设置为null
            activeChat.value = null
          }
        }

        // 保存到 localStorage
        saveChats()

        message.success('对话已删除')
      } catch (error) {
        console.error('删除对话失败:', error)
        message.error('删除失败，请重试')
      }
    }
  })
}

const useExample = (example) => {
  inputMessage.value = example
}

const copyMessage = async (content) => {
  try {
    if (typeof navigator === 'undefined' || !navigator.clipboard) {
      message.warning('当前环境不支持自动复制,请手动复制内容')
      return
    }

    await navigator.clipboard.writeText(normalizeContent(content))
    message.success('已复制到剪贴板')
  } catch (error) {
    console.error('复制失败:', error)
    message.error('复制失败,请手动复制')
  }
}

const downloadMessage = (msg) => {
  try {
    if (typeof window === 'undefined' || typeof document === 'undefined') {
      message.warning('当前环境不支持下载')
      return
    }

    const text = normalizeContent(msg.content)
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${msg.role}-message.txt`
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载失败:', error)
    message.error('下载失败')
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const saveChats = () => {
  try {
    localStorage.setItem('springai-chats', JSON.stringify(chats.value))
    if (activeChat.value) {
      localStorage.setItem('springai-active-chat', activeChat.value)
    } else {
      localStorage.removeItem('springai-active-chat')
    }
  } catch (error) {
    console.error('保存对话失败:', error)
  }
}

const loadChats = () => {
  try {
    const savedChats = localStorage.getItem('springai-chats')
    const savedActiveChat = localStorage.getItem('springai-active-chat')

    if (savedChats) {
      chats.value = JSON.parse(savedChats)
    }

    if (savedActiveChat && chats.value.find(c => c.id === savedActiveChat)) {
      activeChat.value = savedActiveChat
    } else if (chats.value.length > 0) {
      activeChat.value = chats.value[0].id
    }
  } catch (error) {
    console.error('加载聊天记录失败:', error)
    // 清除损坏的数据
    localStorage.removeItem('springai-chats')
    localStorage.removeItem('springai-active-chat')
    chats.value = []
  }
}

const updateChatTitle = (chatId, firstMessage) => {
  const chat = chats.value.find(c => c.id === chatId)
  if (chat && chat.messages.length === 1) {
    chat.title = firstMessage.substring(0, 20) + (firstMessage.length > 20 ? '...' : '')
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || loading.value) return

  // 取消之前的请求
  if (abortController.value) {
    abortController.value.abort()
  }

  if (!activeChat.value) {
    createNewChat()
  }

  const userMessage = inputMessage.value.trim()
  const chat = chats.value.find(c => c.id === activeChat.value)

  const newUserMessage = {
    id: generateId(),
    role: 'user',
    content: userMessage,
    time: getCurrentTime()
  }

  chat.messages.push(newUserMessage)
  updateChatTitle(activeChat.value, userMessage)

  // 清空输入框
  inputMessage.value = ''
  await nextTick()

  loading.value = true
  streamingMessage.value = ''
  scrollToBottom()

  // 创建新的 AbortController
  abortController.value = new AbortController()

  try {
    const response = await fetch('/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ prompt: userMessage }),
      signal: abortController.value.signal
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let fullMessage = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      fullMessage += chunk
      streamingMessage.value = fullMessage
      scrollToBottom()
    }

    chat.messages.push({
      id: generateId(),
      role: 'assistant',
      content: fullMessage || '抱歉,我没有收到响应',
      time: getCurrentTime()
    })

    streamingMessage.value = ''
    saveChats()

  } catch (error) {
    if (error.name === 'AbortError') {
      console.log('请求已取消')
      return
    }

    console.error('发送消息失败:', error)
    let errorMessage = '发送消息失败'

    if (error.message.includes('Failed to fetch')) {
      errorMessage = '无法连接到服务器,请检查网络连接和后端服务'
    } else if (error.message.includes('HTTP error')) {
      errorMessage = `服务器错误: ${error.message}`
    }

    message.error(errorMessage)
    chat.messages.push({
      id: generateId(),
      role: 'assistant',
      content: '抱歉,服务暂时不可用',
      time: getCurrentTime()
    })
    streamingMessage.value = ''
  } finally {
    loading.value = false
    abortController.value = null
    scrollToBottom()
  }
}

const handleKeydown = (e) => {
  if (e.shiftKey) {
    return
  }
  e.preventDefault()
  sendMessage()
}

onMounted(() => {
  loadChats()

  if (chats.value.length === 0) {
    createNewChat()
  }
})
</script>

<style scoped>
.chat-view {
  display: flex;
  height: 100vh;
  background: var(--bg-secondary);
  transition: background var(--transition-normal);
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  transition: background var(--transition-normal);
  max-width: 100%;
}

.chat-header {
  padding: 20px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: border-color var(--transition-normal), background var(--transition-normal);
}

.header-left h1 {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 4px;
  transition: color var(--transition-normal);
}

.header-left p {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
  transition: color var(--transition-normal);
}

.header-right {
  display: flex;
  align-items: center;
}

.chat-content-wrapper {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  background: var(--bg-primary);
  transition: background var(--transition-normal);
}

.chat-content-column {
  width: 100%;
  max-width: 900px;
  display: flex;
  flex-direction: column;
  padding: 0 24px;
  min-height: 100%;
}

.chat-messages {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 32px 0 24px;
}

.message-card {
  width: 100%;
  border-radius: 28px;
  border: 1px solid var(--border-color);
  box-shadow: var(--shadow-md);
  padding: 24px;
  background: var(--bg-primary);
  display: flex;
  flex-direction: column;
  gap: 16px;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.message-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.message-card.user {
  border-color: var(--accent-color);
  box-shadow: var(--shadow-md);
  width: fit-content !important;
  max-width: 80% !important;
  margin-left: auto;
  margin-right: 0;
}

.message-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.meta-name {
  font-weight: 600;
  color: var(--text-primary);
}

.meta-time {
  font-size: 12px;
  color: var(--text-tertiary);
}

.meta-actions {
  display: flex;
  gap: 8px;
}

.meta-btn {
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 4px 14px;
  background: var(--bg-primary);
  font-size: 12px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.meta-btn:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
}

.message-content {
  width: 100%;
  font-size: 15px;
  line-height: 1.65;
  color: var(--text-primary);
}

.message-text {
  white-space: pre-wrap;
  color: inherit;
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-secondary);
  transition: color var(--transition-normal);
}

.empty-icon {
  width: 120px;
  height: 120px;
  margin: 0 auto 24px;
  animation: float 3s ease-in-out infinite;
}

.empty-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.1));
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-10px);
  }
}

.empty-state h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
  transition: color var(--transition-normal);
}

.empty-state p {
  font-size: 14px;
  margin-bottom: 32px;
}

.examples {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
  max-width: 700px;
  margin: 0 auto;
}

.example-card {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 16px 20px;
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: 14px;
  color: var(--text-primary);
  text-align: left;
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.example-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 3px;
  height: 100%;
  background: var(--accent-color);
  transform: scaleY(0);
  transition: transform var(--transition-fast);
}

.example-card:hover {
  border-color: var(--accent-color);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  background: var(--accent-light);
}

.example-card:hover::before {
  transform: scaleY(1);
}

.example-card:active {
  transform: translateY(0);
}

.chat-input-area {
  flex-shrink: 0;
  position: sticky;
  bottom: 0;
  padding: 16px 0 40px;
  background: transparent;
}

.chat-input {
  width: 100%;
  position: relative;
  display: flex;
  align-items: center;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 12px 16px;
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-md);
}

.chat-input:focus-within {
  border-color: var(--accent-color);
  box-shadow: 0 0 0 4px var(--accent-light);
}

.message-input {
  flex: 1;
  border: none !important;
  background: transparent !important;
  box-shadow: none !important;
  padding: 6px 0 !important;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  outline: none !important;
  color: var(--text-primary) !important;
  max-height: 150px;
  overflow-y: auto !important;
}

.input-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 12px;
}

.input-icon {
  color: var(--text-tertiary);
  border-radius: 50% !important;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}

.input-icon:hover {
  color: var(--accent-color);
  background: var(--accent-light);
}

.message-input:focus {
  border: none !important;
  box-shadow: none !important;
  color: var(--text-primary) !important;
}

.message-input::placeholder {
  color: var(--text-tertiary);
}

.send-button {
  width: 36px;
  height: 36px;
  min-width: 36px;
  min-height: 36px;
  border-radius: 50%;
  border: none;
  background: var(--accent-color);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--transition-fast);
  font-size: 16px;
  margin-left: 8px;
  flex-shrink: 0;
}

.send-button:hover:not(:disabled) {
  background: var(--accent-hover);
  transform: scale(1.05);
}

.send-button:active:not(:disabled) {
  transform: scale(0.95);
}

.send-button:disabled {
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
  cursor: not-allowed;
  opacity: 0.5;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-content-column {
    padding: 0 16px;
  }

  .chat-messages {
    padding: 24px 0 16px;
  }

  .chat-input-area {
    padding: 12px 16px 32px;
  }

  .chat-header {
    padding: 16px;
  }

  .message-card {
    padding: 20px;
    border-radius: 22px;
  }

  .meta-actions {
    width: 100%;
    justify-content: flex-end;
  }

  .empty-state {
    padding: 40px 16px;
  }

  .empty-state h2 {
    font-size: 20px;
  }

  .examples {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 480px) {
  .chat-content-column {
    padding: 0 12px;
  }

  .chat-messages {
    padding: 20px 0 12px;
  }

  .chat-input-area {
    padding: 10px 12px 24px;
  }

  .message-card {
    padding: 16px;
    border-radius: 18px;
  }

  .meta-actions {
    justify-content: flex-start;
  }

  .examples {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .example-card {
    padding: 14px 16px;
    font-size: 13px;
  }
}
</style>
