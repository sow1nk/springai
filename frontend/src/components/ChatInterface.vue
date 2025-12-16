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
            <a-select-option value="gemini-advanced">
              <span class="model-name">Gemini Advanced</span>
            </a-select-option>
            <a-select-option value="gemini-pro">
              <span class="model-name">Gemini Pro</span>
            </a-select-option>
            <a-select-option value="gpt-4">
              <span class="model-name">GPT-4</span>
            </a-select-option>
          </a-select>
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
                    <div v-if="message.role === 'user'" v-html="message.content"></div>
                    <MarkdownRenderer v-else :content="message.content" />
                  </div>

                  <!-- Streaming Indicator -->
                  <div v-if="message.streaming" class="streaming-indicator">
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
          <div class="input-actions-left">
            <a-button type="text" class="action-btn" @click="handleUploadImage">
              <PictureOutlined />
            </a-button>
          </div>

          <a-textarea
            v-model:value="inputText"
            :auto-size="{ minRows: 1, maxRows: 6 }"
            placeholder="Ask me anything..."
            class="message-input"
            @pressEnter="handleSendMessage"
          />

          <div class="input-actions-right">
            <a-button type="text" class="action-btn" @click="handleVoiceInput">
              <AudioOutlined />
            </a-button>
            <a-button
              type="primary"
              shape="circle"
              class="send-btn"
              :disabled="!inputText.trim()"
              @click="handleSendMessage"
            >
              <SendOutlined />
            </a-button>
          </div>
        </div>

        <div class="input-footer">
          <span class="disclaimer">AI can make mistakes. Check important info.</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import {
  MenuOutlined,
  MenuFoldOutlined,
  PlusOutlined,
  MessageOutlined,
  SparklesOutlined,
  UserOutlined,
  PictureOutlined,
  AudioOutlined,
  SendOutlined
} from '@ant-design/icons-vue'
import MarkdownRenderer from './MarkdownRenderer.vue'

// Types
interface Message {
  role: 'user' | 'assistant'
  content: string
  streaming?: boolean
}

interface ChatHistory {
  id: string
  title: string
  messages: Message[]
}

interface Suggestion {
  id: number
  icon: string
  text: string
}

// State
const sidebarCollapsed = ref(false)
const selectedModel = ref('gemini-advanced')
const inputText = ref('')
const messages = ref<Message[]>([])
const chatAreaRef = ref<HTMLElement | null>(null)
const currentChatId = ref('default')

// Suggestions
const suggestions = ref<Suggestion[]>([
  { id: 1, icon: '✍️', text: 'Help me write a professional email' },
  { id: 2, icon: '✈️', text: 'Plan a trip to Japan' },
  { id: 3, icon: '💡', text: 'Brainstorm creative ideas for a startup' },
  { id: 4, icon: '📊', text: 'Explain data visualization best practices' }
])

// Chat History
const chatHistory = ref<ChatHistory[]>([
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

const selectSuggestion = (text: string) => {
  inputText.value = text
  handleSendMessage()
}

const handleSendMessage = async (e?: KeyboardEvent) => {
  if (e && !e.ctrlKey && !e.metaKey) {
    e.preventDefault()
  }

  if (!inputText.value.trim()) return

  const userMessage: Message = {
    role: 'user',
    content: inputText.value.trim()
  }

  messages.value.push(userMessage)
  const userInput = inputText.value
  inputText.value = ''

  scrollToBottom()

  // Update chat history title if first message
  if (messages.value.length === 1) {
    const currentChat = chatHistory.value.find(chat => chat.id === currentChatId.value)
    if (currentChat) {
      currentChat.title = userInput.substring(0, 30) + (userInput.length > 30 ? '...' : '')
    }
  }

  // Simulate AI response with streaming effect
  await simulateAIResponse()
}

const simulateAIResponse = async () => {
  const aiMessage: Message = {
    role: 'assistant',
    content: '',
    streaming: true
  }

  messages.value.push(aiMessage)
  scrollToBottom()

  // Simulated AI response text
  const fullResponse = `I understand you're looking for assistance. Here's a comprehensive response:

## Key Points

1. **First Point**: This is an important consideration that addresses your query.
2. **Second Point**: Here's another aspect to think about with detailed explanation.
3. **Third Point**: Additional insights that might be helpful.

### Example Code

\`\`\`javascript
function example() {
  console.log("This is a code example");
  return "Hello, World!";
}
\`\`\`

### Additional Information

Here's some more context and details that expand on the main points. This demonstrates the streaming effect as the text appears gradually.

Let me know if you need any clarification or have follow-up questions!`

  // Simulate streaming by adding characters progressively
  const words = fullResponse.split(' ')
  for (let i = 0; i < words.length; i++) {
    aiMessage.content += (i > 0 ? ' ' : '') + words[i]
    scrollToBottom()
    await new Promise(resolve => setTimeout(resolve, 50))
  }

  aiMessage.streaming = false
  scrollToBottom()
}

const startNewChat = () => {
  const newChat: ChatHistory = {
    id: `chat-${Date.now()}`,
    title: 'New Chat',
    messages: []
  }
  chatHistory.value.unshift(newChat)
  currentChatId.value = newChat.id
  messages.value = []
}

const loadChat = (chatId: string) => {
  const chat = chatHistory.value.find(c => c.id === chatId)
  if (chat) {
    currentChatId.value = chatId
    messages.value = [...chat.messages]
    scrollToBottom()
  }
}

const handleUploadImage = () => {
  console.log('Upload image clicked')
  // Implement image upload logic
}

const handleVoiceInput = () => {
  console.log('Voice input clicked')
  // Implement voice input logic
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
  font-size: 18px;
  color: var(--text-secondary);
  transition: color var(--transition-theme);
}

.sidebar-content {
  padding: 16px;
  flex: 1;
  overflow-y: auto;
}

.new-chat-btn {
  border-radius: 24px;
  height: 40px;
  margin-bottom: 16px;
  font-weight: 500;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.chat-history {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.history-item:hover {
  background: var(--bg-tertiary);
}

.history-item.active {
  background: var(--accent-light);
  color: var(--accent-color);
}

.history-icon {
  font-size: 16px;
  color: var(--text-secondary);
  transition: color var(--transition-theme);
}

.history-title {
  flex: 1;
  font-size: 14px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Main Content */
.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--bg-primary);
  position: relative;
  transition: background var(--transition-theme);
}

/* Top Bar */
.top-bar {
  height: 64px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  padding: 0 24px;
  background: var(--bg-primary);
  transition: background var(--transition-theme), border-color var(--transition-theme);
}

.top-bar-content {
  width: 100%;
  max-width: 800px;
  margin: 0 auto;
}

.model-selector {
  width: 200px;
}

.model-selector :deep(.ant-select-selector) {
  border-radius: 20px;
  border: 1px solid var(--border-color);
  padding: 4px 12px;
  transition: border-color var(--transition-theme);
}

.model-name {
  font-weight: 500;
  color: var(--text-primary);
  transition: color var(--transition-theme);
}

/* Chat Area */
.chat-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  scroll-behavior: smooth;
}

.chat-messages {
  max-width: 800px;
  margin: 0 auto;
}

/* Welcome Screen */
.welcome-screen {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: calc(100vh - 280px);
}

.welcome-content {
  text-align: center;
  max-width: 700px;
}

.welcome-icon {
  font-size: 64px;
  margin-bottom: 24px;
  color: var(--accent-color);
  transition: color var(--transition-theme);
}

.welcome-title {
  font-size: 32px;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 12px;
  transition: color var(--transition-theme);
}

.welcome-subtitle {
  font-size: 16px;
  color: var(--text-secondary);
  margin-bottom: 40px;
  transition: color var(--transition-theme);
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 16px;
  margin-top: 32px;
}

.suggestion-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1), background var(--transition-theme), border-color var(--transition-theme);
  text-align: left;
}

.suggestion-card:hover {
  background: var(--bg-primary);
  border-color: var(--accent-color);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.suggestion-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.suggestion-text {
  font-size: 15px;
  color: var(--text-primary);
  line-height: 1.5;
  transition: color var(--transition-theme);
}

/* Messages */
.messages-container {
  display: flex;
  flex-direction: column;
  gap: 32px;
  padding-bottom: 24px;
}

.message-wrapper {
  display: flex;
  animation: messageSlideIn 0.3s ease-out;
}

@keyframes messageSlideIn {
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
  0%, 60%, 100% {
    opacity: 0.3;
    transform: scale(0.8);
  }
  30% {
    opacity: 1;
    transform: scale(1);
  }
}

/* Input Area */
.input-area {
  padding: 16px 24px 24px;
  background: var(--bg-primary);
  border-top: 1px solid var(--border-color);
  transition: background var(--transition-theme), border-color var(--transition-theme);
}

.input-container {
  max-width: 800px;
  margin: 0 auto;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 24px;
  padding: 12px 16px;
  display: flex;
  align-items: flex-end;
  gap: 8px;
  transition: all 0.2s, background var(--transition-theme), border-color var(--transition-theme);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 4px rgba(0, 0, 0, 0.04);
}

.input-container:focus-within {
  border-color: var(--accent-color);
  box-shadow: 0 4px 12px rgba(25, 118, 210, 0.2), 0 2px 6px rgba(25, 118, 210, 0.12);
}

.input-actions-left,
.input-actions-right {
  display: flex;
  gap: 4px;
  align-items: center;
}

.action-btn {
  color: var(--text-secondary);
  font-size: 18px;
  transition: color var(--transition-theme);
}

.message-input {
  flex: 1;
  border: none;
  background: transparent;
  resize: none;
  font-size: 15px;
  padding: 4px 8px;
}

.message-input :deep(.ant-input) {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
  color: var(--text-primary);
  transition: color var(--transition-theme);
}

.message-input :deep(.ant-input:focus) {
  box-shadow: none;
}

.send-btn {
  width: 36px;
  height: 36px;
  min-width: 36px;
  background: var(--accent-color);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--transition-theme), transform var(--transition-fast);
}

.send-btn:disabled {
  background: var(--bg-tertiary);
  color: var(--text-tertiary);
}

.send-btn:not(:disabled):hover {
  background: var(--accent-hover);
  transform: scale(1.05);
}

.input-footer {
  max-width: 800px;
  margin: 8px auto 0;
  text-align: center;
}

.disclaimer {
  font-size: 12px;
  color: var(--text-tertiary);
  transition: color var(--transition-theme);
}

/* Scrollbar Styling */
.chat-area::-webkit-scrollbar,
.sidebar-content::-webkit-scrollbar {
  width: 8px;
}

.chat-area::-webkit-scrollbar-track,
.sidebar-content::-webkit-scrollbar-track {
  background: transparent;
}

.chat-area::-webkit-scrollbar-thumb,
.sidebar-content::-webkit-scrollbar-thumb {
  background: var(--scrollbar-thumb);
  border-radius: 4px;
  transition: background var(--transition-theme);
}

.chat-area::-webkit-scrollbar-thumb:hover,
.sidebar-content::-webkit-scrollbar-thumb:hover {
  background: var(--scrollbar-thumb-hover);
}

/* Responsive Design */
@media (max-width: 768px) {
  .sidebar {
    position: absolute;
    z-index: 100;
    height: 100%;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  }

  .sidebar.collapsed {
    transform: translateX(-100%);
  }

  .welcome-title {
    font-size: 24px;
  }

  .suggestion-grid {
    grid-template-columns: 1fr;
  }

  .input-area {
    padding: 12px;
  }
}
</style>
