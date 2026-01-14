<template>
  <div class="chat-interface">
    <!-- Sidebar -->
    <div :class="['sidebar', { collapsed: sidebarCollapsed }]">
      <div class="sidebar-header">
        <a-button
          type="text"
          class="toggle-btn"
          @click="sidebarCollapsed = !sidebarCollapsed"
        >
          <MenuOutlined v-if="sidebarCollapsed" />
          <MenuFoldOutlined v-else />
        </a-button>
        <h3 v-if="!sidebarCollapsed">Chat History</h3>
      </div>

      <div v-if="!sidebarCollapsed" class="sidebar-content">
        <a-button
          type="primary"
          block
          class="new-chat-btn"
          @click="startNewChat"
        >
          <PlusOutlined /> New Chat
        </a-button>

        <div class="chat-history">
          <div
            v-for="chat in chatHistory"
            :key="chat.id"
            :class="['history-item', { active: chat.id === currentChatId }]"
            @click="loadChat(chat.id)"
          >
            <MessageOutlined class="history-icon" />
            <span class="history-title">{{ chat.title }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="main-content">
      <!-- Top Bar -->
      <div class="top-bar">
        <div class="top-bar-content">
          <a-select
            v-model:value="selectedModel"
            class="model-selector"
            :bordered="false"
          >
            <a-select-option value="deepseek">
              <span class="model-name">DeepSeek</span>
            </a-select-option>
            <a-select-option value="qwen">
              <span class="model-name">Qwen</span>
            </a-select-option>
            <a-select-option value="zhipu">
              <span class="model-name">ZhipuAI</span>
            </a-select-option>
          </a-select>

          <div class="user-section">
            <span class="username">{{ userInfo.username }}</span>
            <a-button type="text" @click="handleLogout" class="logout-btn">
              <LogoutOutlined />
            </a-button>
          </div>
        </div>
      </div>

      <!-- Chat Area -->
      <div class="chat-area" ref="chatAreaRef">
        <div class="chat-messages">
          <!-- Welcome Screen -->
          <div v-if="messages.length === 0" class="welcome-screen">
            <div class="welcome-content">
              <div class="welcome-icon">
                <SparklesOutlined />
              </div>
              <h1 class="welcome-title">Hello! How can I help you today?</h1>
              <p class="welcome-subtitle">Choose a suggestion or type your own message</p>

              <div class="suggestion-grid">
                <div
                  v-for="suggestion in suggestions"
                  :key="suggestion.id"
                  class="suggestion-card"
                  @click="selectSuggestion(suggestion.text)"
                >
                  <div class="suggestion-icon">{{ suggestion.icon }}</div>
                  <div class="suggestion-text">{{ suggestion.text }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Messages -->
          <div v-else class="messages-container">
            <div
              v-for="(message, index) in messages"
              :key="index"
              :class="['message-wrapper', message.role]"
            >
              <div class="message-content">
                <!-- Avatar -->
                <div class="message-avatar">
                  <UserOutlined v-if="message.role === 'user'" />
                  <SparklesOutlined v-else class="ai-icon" />
                </div>

                <!-- Message Body -->
                <div class="message-body">
                  <div class="message-role-label">
                    {{ message.role === 'user' ? 'You' : 'AI' }}
                  </div>
                  <div class="message-text">
                    <div v-if="message.role === 'user'">{{ message.content }}</div>
                    <MarkdownRenderer v-else :content="message.content" />
                  </div>

                  <!-- Loading Indicator -->
                  <div v-if="message.loading" class="streaming-indicator">
                    <span class="dot"></span>
                    <span class="dot"></span>
                    <span class="dot"></span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="input-area">
        <div class="input-container">
          <a-textarea
            v-model:value="inputMessage"
            :auto-size="{ minRows: 1, maxRows: 6 }"
            placeholder="Ask me anything..."
            class="message-input"
            @pressEnter="handleSend"
            :disabled="isLoading"
          />

          <a-button
            type="primary"
            shape="circle"
            class="send-btn"
            :disabled="!inputMessage.trim() || isLoading"
            @click="handleSend"
          >
            <SendOutlined />
          </a-button>
        </div>

        <div class="input-footer">
          <span class="disclaimer">AI can make mistakes. Check important info.</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { message as antMessage } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import {
  MenuOutlined,
  MenuFoldOutlined,
  PlusOutlined,
  MessageOutlined,
  UserOutlined,
  SendOutlined,
  SparklesOutlined,
  LogoutOutlined
} from '@ant-design/icons-vue'
import MarkdownRenderer from './MarkdownRenderer.vue'
import { sendMessage } from '../api/chat'
import { logout, getUserInfo } from '../api/auth'

const router = useRouter()
const chatAreaRef = ref(null)
const sidebarCollapsed = ref(false)
const inputMessage = ref('')
const messages = ref([])
const isLoading = ref(false)
const selectedModel = ref('deepseek')
const currentConversationId = ref(null)
const currentChatId = ref('default')
const userInfo = ref(getUserInfo())

// Suggestions
const suggestions = ref([
  { id: 1, icon: '✍️', text: 'Help me write a professional email' },
  { id: 2, icon: '✈️', text: 'Plan a trip to Japan' },
  { id: 3, icon: '💡', text: 'Brainstorm creative ideas for a startup' },
  { id: 4, icon: '📊', text: 'Explain data visualization best practices' }
])

// Chat History
const chatHistory = ref([
  {
    id: 'default',
    title: 'New Chat',
    messages: []
  }
])

// Methods
const scrollToBottom = () => {
  nextTick(() => {
    if (chatAreaRef.value) {
      chatAreaRef.value.scrollTop = chatAreaRef.value.scrollHeight
    }
  })
}

const selectSuggestion = (text) => {
  inputMessage.value = text
  handleSend()
}

const handleSend = async (e) => {
  if (e && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
  }

  if (!inputMessage.value.trim() || isLoading.value) return

  const userMessage = {
    role: 'user',
    content: inputMessage.value.trim()
  }

  messages.value.push(userMessage)
  const currentMessage = inputMessage.value
  inputMessage.value = ''
  isLoading.value = true

  scrollToBottom()

  // Update chat history title if first message
  if (messages.value.length === 1) {
    const currentChat = chatHistory.value.find(chat => chat.id === currentChatId.value)
    if (currentChat) {
      currentChat.title = currentMessage.substring(0, 30) + (currentMessage.length > 30 ? '...' : '')
    }
  }

  // Add loading message
  const loadingMessage = {
    role: 'assistant',
    content: '',
    loading: true
  }
  messages.value.push(loadingMessage)
  scrollToBottom()

  try {
    const response = await sendMessage(
      currentMessage,
      selectedModel.value,
      currentConversationId.value
    )

    // Remove loading message and add actual response
    messages.value.pop()

    const assistantMessage = {
      role: 'assistant',
      content: response
    }

    messages.value.push(assistantMessage)
    scrollToBottom()
  } catch (error) {
    console.error('Send message failed:', error)
    messages.value.pop() // Remove loading message
    antMessage.error('Failed to send message')
  } finally {
    isLoading.value = false
  }
}

const startNewChat = () => {
  const newChat = {
    id: `chat-${Date.now()}`,
    title: 'New Chat',
    messages: []
  }
  chatHistory.value.unshift(newChat)
  currentChatId.value = newChat.id
  currentConversationId.value = null
  messages.value = []
  antMessage.success('Started new chat')
}

const loadChat = (chatId) => {
  const chat = chatHistory.value.find(c => c.id === chatId)
  if (chat) {
    currentChatId.value = chatId
    messages.value = [...chat.messages]
    scrollToBottom()
  }
}

const handleLogout = () => {
  logout()
  router.push('/auth')
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-interface {
  display: flex;
  height: 100vh;
  background: var(--bg-primary);
  overflow: hidden;
  transition: background var(--transition-theme);
}

/* Sidebar */
.sidebar {
  width: 280px;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1), background var(--transition-theme), border-color var(--transition-theme);
}

.sidebar.collapsed {
  width: 60px;
}

.sidebar-header {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid var(--border-color);
  transition: border-color var(--transition-theme);
}

.sidebar-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  transition: color var(--transition-theme);
}

.toggle-btn {
  color: var(--text-secondary);
  transition: color var(--transition-fast);
}

.toggle-btn:hover {
  color: var(--primary-color);
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.new-chat-btn {
  margin-bottom: 16px;
  height: 40px;
  border-radius: 8px;
  font-weight: 500;
}

.chat-history {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background var(--transition-fast);
  background: transparent;
}

.history-item:hover {
  background: var(--bg-tertiary);
}

.history-item.active {
  background: var(--bg-tertiary);
}

.history-icon {
  color: var(--text-secondary);
  font-size: 16px;
}

.history-title {
  flex: 1;
  font-size: 14px;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Main Content */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* Top Bar */
.top-bar {
  padding: 16px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-secondary);
  transition: background var(--transition-theme), border-color var(--transition-theme);
}

.top-bar-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.model-selector {
  min-width: 200px;
}

.user-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.logout-btn {
  color: var(--text-secondary);
  transition: color var(--transition-fast);
}

.logout-btn:hover {
  color: var(--primary-color);
}

/* Chat Area */
.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.chat-messages {
  max-width: 900px;
  margin: 0 auto;
}

/* Welcome Screen */
.welcome-screen {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100%;
}

.welcome-content {
  text-align: center;
  max-width: 600px;
}

.welcome-icon {
  font-size: 64px;
  margin-bottom: 24px;
  color: var(--primary-color);
}

.welcome-title {
  font-size: 32px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 12px;
}

.welcome-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 32px;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 32px;
}

.suggestion-card {
  padding: 20px;
  border-radius: 12px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-align: left;
}

.suggestion-card:hover {
  background: var(--bg-tertiary);
  border-color: var(--primary-color);
  transform: translateY(-2px);
}

.suggestion-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.suggestion-text {
  font-size: 14px;
  color: var(--text-primary);
  line-height: 1.5;
}

/* Messages */
.messages-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.message-wrapper {
  animation: fadeInUp 0.3s ease-out;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-content {
  display: flex;
  gap: 16px;
  width: 100%;
}

.message-avatar {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  transition: background var(--transition-theme), color var(--transition-theme);
}

.user .message-avatar {
  background: var(--accent-color);
  color: #ffffff;
}

.assistant .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #ffffff;
}

.ai-icon {
  animation: sparkle 2s ease-in-out infinite;
}

@keyframes sparkle {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.message-body {
  flex: 1;
  min-width: 0;
}

.message-role-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 6px;
  transition: color var(--transition-theme);
}

.message-text {
  font-size: 15px;
  line-height: 1.6;
  color: var(--text-primary);
  word-wrap: break-word;
  transition: color var(--transition-theme);
}

.user .message-text {
  background: var(--user-bubble-bg);
  padding: 12px 16px;
  border-radius: 18px;
  display: inline-block;
  max-width: fit-content;
  transition: background var(--transition-theme);
}

/* Streaming Indicator */
.streaming-indicator {
  display: flex;
  gap: 4px;
  margin-top: 8px;
}

.streaming-indicator .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--text-secondary);
  animation: pulse 1.4s ease-in-out infinite;
  transition: background var(--transition-theme);
}

.streaming-indicator .dot:nth-child(2) {
  animation-delay: 0.2s;
}

.streaming-indicator .dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes pulse {
  0%, 80%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

/* Input Area */
.input-area {
  padding: 16px 24px;
  border-top: 1px solid var(--border-color);
  background: var(--bg-secondary);
  transition: background var(--transition-theme), border-color var(--transition-theme);
}

.input-container {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.message-input {
  flex: 1;
  border-radius: 12px;
  font-size: 15px;
  resize: none;
}

.send-btn {
  flex-shrink: 0;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.input-footer {
  max-width: 900px;
  margin: 8px auto 0;
  text-align: center;
}

.disclaimer {
  font-size: 12px;
  color: var(--text-tertiary);
}
</style>
