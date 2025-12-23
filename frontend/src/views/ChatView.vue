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
          <div class="header-top">
            <img :src="panda" />
            <p>支持数学计算和MySQL数据库查询</p>
          </div>
          <a-select
            v-model:value="selectedModel"
            class="model-selector"
            :bordered="true"
            style="width: 140px;"
          >
            <a-select-option value="deepseek">
              <span class="model-name">DeepSeek</span>
            </a-select-option>
            <a-select-option value="qwen" disabled>
              <a-tooltip title="当前模型暂未开放">
                <span class="model-name">Qwen</span>
              </a-tooltip>
            </a-select-option>
          <a-select-option value="zhipu">
              <span class="model-name">Zhipu</span>
            </a-select-option>
          </a-select>
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
                <a-button
                  v-if="message.role === 'user'"
                  type="text"
                  size="small"
                  class="copy-btn"
                  @click="copyMessage(message.content)"
                >
                  <template #icon>
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M6.14923 4.02032C7.11191 4.02032 7.87977 4.02017 8.49591 4.07599C9.12122 4.1327 9.65786 4.25188 10.1414 4.53107C10.7201 4.8653 11.2008 5.34591 11.535 5.92462C11.8142 6.40818 11.9333 6.94482 11.9901 7.57013C12.0459 8.18625 12.0457 8.9542 12.0457 9.91681C12.0457 10.8795 12.0459 11.6474 11.9901 12.2635C11.9333 12.8888 11.8142 13.4254 11.535 13.909C11.2008 14.4877 10.7201 14.9683 10.1414 15.3026C9.65786 15.5817 9.12122 15.7009 8.49591 15.7576C7.87977 15.8134 7.1119 15.8133 6.14923 15.8133C5.18661 15.8133 4.41868 15.8134 3.80255 15.7576C3.17724 15.7009 2.6406 15.5817 2.15704 15.3026C1.57834 14.9684 1.09772 14.4877 0.763489 13.909C0.484305 13.4254 0.365123 12.8888 0.308411 12.2635C0.252587 11.6474 0.252747 10.8795 0.252747 9.91681C0.252747 8.95419 0.252603 8.18625 0.308411 7.57013C0.365123 6.94482 0.484305 6.40818 0.763489 5.92462C1.09771 5.3459 1.57833 4.86529 2.15704 4.53107C2.6406 4.25188 3.17724 4.1327 3.80255 4.07599C4.41868 4.02018 5.1866 4.02032 6.14923 4.02032ZM6.14923 5.37775C5.16175 5.37775 4.46628 5.37761 3.9256 5.42657C3.39428 5.47473 3.07853 5.56574 2.83575 5.70587C2.46313 5.92106 2.15348 6.23071 1.93829 6.60333C1.79817 6.84611 1.70715 7.16185 1.659 7.69318C1.61004 8.23385 1.61017 8.92934 1.61017 9.91681C1.61017 10.9043 1.61002 11.5998 1.659 12.1404C1.70715 12.6717 1.79817 12.9875 1.93829 13.2303C2.15349 13.6029 2.46315 13.9126 2.83575 14.1277C3.07853 14.2679 3.39428 14.3589 3.9256 14.407C4.46628 14.456 5.16176 14.4559 6.14923 14.4559C7.13675 14.4559 7.83218 14.456 8.37286 14.407C8.90419 14.3589 9.21993 14.2679 9.46271 14.1277C9.83529 13.9126 10.145 13.6029 10.3602 13.2303C10.5003 12.9875 10.5913 12.6718 10.6395 12.1404C10.6884 11.5998 10.6883 10.9043 10.6883 9.91681C10.6883 8.92935 10.6884 8.23385 10.6395 7.69318C10.5913 7.16185 10.5003 6.84611 10.3602 6.60333C10.145 6.23072 9.8353 5.92107 9.46271 5.70587C9.21993 5.56574 8.90418 5.47473 8.37286 5.42657C7.83218 5.3776 7.13676 5.37775 6.14923 5.37775ZM9.80157 0.367981C10.7637 0.367981 11.5313 0.367886 12.1473 0.423645C12.7725 0.480313 13.3093 0.598765 13.7928 0.877747C14.3716 1.21192 14.852 1.69355 15.1863 2.27228C15.4655 2.75575 15.5857 3.29165 15.6424 3.91681C15.6982 4.53301 15.6971 5.30161 15.6971 6.26447V7.8299C15.6971 8.29265 15.6989 8.58994 15.6649 8.84845C15.4667 10.3525 14.4009 11.5738 12.9832 11.9988V10.5467C13.6973 10.1903 14.2104 9.49662 14.3192 8.67169C14.3387 8.52348 14.3406 8.3358 14.3406 7.8299V6.26447C14.3406 5.27707 14.3398 4.58149 14.2908 4.04083C14.2427 3.50969 14.1526 3.19373 14.0125 2.95099C13.7974 2.5785 13.4875 2.2687 13.1151 2.05353C12.8723 1.91347 12.5563 1.82237 12.0252 1.77423C11.4846 1.72528 10.7888 1.7254 9.80157 1.7254H7.71466C6.75614 1.72559 5.92659 2.27697 5.52325 3.07892H4.07013C4.54215 1.51132 5.99314 0.368192 7.71466 0.367981H9.80157Z" fill="currentColor"></path></svg>
                  </template>
                </a-button>
              </div>
              <div class="message-content">
                <MarkdownRenderer
                  v-if="message.role === 'assistant'"
                  :content="message.content"
                />
                <div v-else class="message-text">{{ message.content }}</div>
              </div>
              <div v-if="message.role === 'assistant'" class="message-actions">
                <a-button
                  type="text"
                  size="small"
                  class="copy-btn"
                  @click="copyMessage(message.content)"
                >
                  <template #icon>
                    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M6.14923 4.02032C7.11191 4.02032 7.87977 4.02017 8.49591 4.07599C9.12122 4.1327 9.65786 4.25188 10.1414 4.53107C10.7201 4.8653 11.2008 5.34591 11.535 5.92462C11.8142 6.40818 11.9333 6.94482 11.9901 7.57013C12.0459 8.18625 12.0457 8.9542 12.0457 9.91681C12.0457 10.8795 12.0459 11.6474 11.9901 12.2635C11.9333 12.8888 11.8142 13.4254 11.535 13.909C11.2008 14.4877 10.7201 14.9683 10.1414 15.3026C9.65786 15.5817 9.12122 15.7009 8.49591 15.7576C7.87977 15.8134 7.1119 15.8133 6.14923 15.8133C5.18661 15.8133 4.41868 15.8134 3.80255 15.7576C3.17724 15.7009 2.6406 15.5817 2.15704 15.3026C1.57834 14.9684 1.09772 14.4877 0.763489 13.909C0.484305 13.4254 0.365123 12.8888 0.308411 12.2635C0.252587 11.6474 0.252747 10.8795 0.252747 9.91681C0.252747 8.95419 0.252603 8.18625 0.308411 7.57013C0.365123 6.94482 0.484305 6.40818 0.763489 5.92462C1.09771 5.3459 1.57833 4.86529 2.15704 4.53107C2.6406 4.25188 3.17724 4.1327 3.80255 4.07599C4.41868 4.02018 5.1866 4.02032 6.14923 4.02032ZM6.14923 5.37775C5.16175 5.37775 4.46628 5.37761 3.9256 5.42657C3.39428 5.47473 3.07853 5.56574 2.83575 5.70587C2.46313 5.92106 2.15348 6.23071 1.93829 6.60333C1.79817 6.84611 1.70715 7.16185 1.659 7.69318C1.61004 8.23385 1.61017 8.92934 1.61017 9.91681C1.61017 10.9043 1.61002 11.5998 1.659 12.1404C1.70715 12.6717 1.79817 12.9875 1.93829 13.2303C2.15349 13.6029 2.46315 13.9126 2.83575 14.1277C3.07853 14.2679 3.39428 14.3589 3.9256 14.407C4.46628 14.456 5.16176 14.4559 6.14923 14.4559C7.13675 14.4559 7.83218 14.456 8.37286 14.407C8.90419 14.3589 9.21993 14.2679 9.46271 14.1277C9.83529 13.9126 10.145 13.6029 10.3602 13.2303C10.5003 12.9875 10.5913 12.6718 10.6395 12.1404C10.6884 11.5998 10.6883 10.9043 10.6883 9.91681C10.6883 8.92935 10.6884 8.23385 10.6395 7.69318C10.5913 7.16185 10.5003 6.84611 10.3602 6.60333C10.145 6.23072 9.8353 5.92107 9.46271 5.70587C9.21993 5.56574 8.90418 5.47473 8.37286 5.42657C7.83218 5.3776 7.13676 5.37775 6.14923 5.37775ZM9.80157 0.367981C10.7637 0.367981 11.5313 0.367886 12.1473 0.423645C12.7725 0.480313 13.3093 0.598765 13.7928 0.877747C14.3716 1.21192 14.852 1.69355 15.1863 2.27228C15.4655 2.75575 15.5857 3.29165 15.6424 3.91681C15.6982 4.53301 15.6971 5.30161 15.6971 6.26447V7.8299C15.6971 8.29265 15.6989 8.58994 15.6649 8.84845C15.4667 10.3525 14.4009 11.5738 12.9832 11.9988V10.5467C13.6973 10.1903 14.2104 9.49662 14.3192 8.67169C14.3387 8.52348 14.3406 8.3358 14.3406 7.8299V6.26447C14.3406 5.27707 14.3398 4.58149 14.2908 4.04083C14.2427 3.50969 14.1526 3.19373 14.0125 2.95099C13.7974 2.5785 13.4875 2.2687 13.1151 2.05353C12.8723 1.91347 12.5563 1.82237 12.0252 1.77423C11.4846 1.72528 10.7888 1.7254 9.80157 1.7254H7.71466C6.75614 1.72559 5.92659 2.27697 5.52325 3.07892H4.07013C4.54215 1.51132 5.99314 0.368192 7.71466 0.367981H9.80157Z" fill="currentColor"></path></svg>
                  </template>
                </a-button>
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
        </div>
      </div>

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
</template>

<script setup>
import { ref, computed, nextTick, onMounted, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { ArrowUpOutlined, UserOutlined, SettingOutlined, LogoutOutlined, ExclamationCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import Sidebar from '../components/Sidebar.vue'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'
import { sendChatMessageStream } from '../api/request.js'

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
const SpringAIAvatar = ref("/images/spring-logo.svg")
const selectedModel = ref('deepseek') // 默认为deepseek

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
    const fullMessage = await sendChatMessageStream(
      userMessage,
      selectedModel.value,
      (chunk, fullMsg) => {
        streamingMessage.value = fullMsg
        scrollToBottom()
      },
      abortController.value.signal
    )

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

.header-left {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.header-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-top img {
  width: 40px;
  height: 40px;
}

.header-top p {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
  transition: color var(--transition-normal);
}

.header-right {
  display: flex;
  align-items: center;
}

.model-selector-wrapper {
  margin-right: 16px;
}

.model-selector {
  width: 150px;
  border-radius: 8px;
}

/* 禁用选项样式 - 浅色模式 */
.model-selector :deep(.ant-select-item-option-disabled) {
  color: rgba(0, 0, 0, 0.25) !important;
  cursor: not-allowed !important;
}

.model-selector :deep(.ant-select-item-option-disabled .model-name) {
  color: rgba(0, 0, 0, 0.25) !important;
}

/* 禁用选项样式 - 深色模式 */
[data-theme="dark"] .model-selector :deep(.ant-select-item-option-disabled) {
  color: rgba(255, 255, 255, 0.25) !important;
}

[data-theme="dark"] .model-selector :deep(.ant-select-item-option-disabled .model-name) {
  color: rgba(255, 255, 255, 0.25) !important;
}

.model-selector :deep(.ant-select-selector) {
  border-radius: 8px !important;
  border: 1px solid var(--border-color) !important;
  background: var(--bg-secondary) !important;
  transition: all var(--transition-fast);
  padding: 4px 12px !important;
  height: auto !important;
}

.model-selector:hover :deep(.ant-select-selector) {
  border-color: var(--accent-color) !important;
  background: var(--bg-primary) !important;
}

.model-selector :deep(.ant-select-focused .ant-select-selector) {
  border-color: var(--accent-color) !important;
  box-shadow: 0 0 0 2px var(--accent-light) !important;
}

.model-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.model-icon {
  font-size: 16px;
}

.model-name {
  font-weight: 500;
  color: var(--text-primary);
  font-size: 14px;
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
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 32px 0 24px;
}

.message-card {
  width: 100%;
  border-radius: 0;
  border: none;
  padding: 12px 0;
  background: transparent;
  color: var(--assistant-bubble-text);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.message-card.user {
  width: fit-content;
  max-width: 85%;
  border-radius: 16px;
  border: 1px solid var(--user-bubble-border);
  padding: 12px 16px;
  background: var(--user-bubble-bg);
  color: var(--user-bubble-text);
  margin-left: auto;
  margin-right: 0;
}

.message-card.user .meta-name,
.message-card.user .meta-time {
  color: var(--user-bubble-text);
}

.message-card.user .meta-time {
  opacity: 0.7;
}

.message-card.user .meta-btn {
  background: transparent;
  border-color: var(--user-bubble-border);
  color: var(--user-bubble-text);
}

.message-card.user .meta-btn:hover {
  background: rgba(0, 0, 0, 0.05);
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta-info {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.meta-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.meta-time {
  font-size: 11px;
  color: var(--text-tertiary);
}

.copy-btn {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  width: 28px !important;
  height: 28px !important;
  padding: 0 !important;
  color: var(--text-tertiary) !important;
  border-radius: 6px !important;
}

.copy-btn:hover {
  color: var(--accent-color) !important;
  background: var(--accent-light) !important;
}

.copy-btn svg {
  width: 16px;
  height: 16px;
}

.message-card.user .copy-btn {
  color: var(--user-bubble-text) !important;
  opacity: 0.7;
}

.message-card.user .copy-btn:hover {
  opacity: 1;
  background: rgba(0, 0, 0, 0.08) !important;
}

.message-actions {
  display: flex;
  justify-content: flex-start;
  margin-top: 8px;
}

.message-content {
  font-size: 15px;
  line-height: 1.65;
  color: var(--assistant-bubble-text);
}

.message-card.user .message-content {
  color: var(--user-bubble-text);
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

.chat-input {
  flex-shrink: 0;
  width: calc(100% - 48px);
  max-width: 900px;
  margin: 0 auto 24px;
  position: relative;
  display: flex;
  align-items: center;
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: 16px;
  padding: 12px 16px;
  transition: all var(--transition-fast);
  box-shadow: var(--shadow-md);
  box-sizing: border-box;
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

  .chat-input {
    width: calc(100% - 32px);
    margin-bottom: 20px;
  }

  .chat-header {
    padding: 16px;
  }

  .message-card {
    padding: 10px 14px;
    border-radius: 14px;
    max-width: 90%;
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

  .chat-input {
    width: calc(100% - 24px);
    margin-bottom: 16px;
  }

  .message-card {
    padding: 8px 12px;
    border-radius: 12px;
    max-width: 95%;
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
