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
          <img :src="panda" class="header-logo" />
          <a-popover
            v-model:open="modelDropdownOpen"
            trigger="click"
            placement="bottomLeft"
            :arrow="false"
            overlay-class-name="model-dropdown-popover"
          >
            <button class="model-trigger" @click.stop>
              <span class="model-trigger-name">{{ modelMap[selectedModel]?.name || selectedModel }}</span>
              <svg class="model-trigger-arrow" :class="{ open: modelDropdownOpen }" width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path d="M3 4.5L6 7.5L9 4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <template #content>
              <div class="model-dropdown-panel">
                <div
                  v-for="model in modelList"
                  :key="model.value"
                  class="model-dropdown-item"
                  :class="{ selected: selectedModel === model.value }"
                  @click="selectModel(model.value)"
                >
                  <div class="model-item-info">
                    <span class="model-item-name">{{ model.name }}</span>
                    <span class="model-item-desc">{{ model.desc }}</span>
                  </div>
                  <svg v-if="selectedModel === model.value" class="model-item-check" width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path d="M3 8.5L6.5 12L13 4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </div>
              </div>
            </template>
          </a-popover>
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
              <a-menu @click="handleMenuClick">
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

            <!-- 工作流步骤 + 流式响应 (合并为统一卡片) -->
            <div v-if="loading || streamingNodes.length > 0" class="message-card assistant streaming-card">
              <div class="message-meta">
                <div class="meta-left">
                  <a-avatar :src="AIAvatar" :style="{ flexShrink: 0 }"></a-avatar>
                  <div class="meta-info">
                    <span class="meta-name">Spring AI</span>
                    <span class="meta-time">{{ selectedModel }}</span>
                  </div>
                </div>
              </div>
              <div class="message-content">
                <!-- 思考过程 (可折叠时间线) -->
                <div v-if="streamingNodes.length > 0" class="thinking-process">
                  <div class="thinking-header" @click="thinkingExpanded = !thinkingExpanded">
                    <span class="thinking-arrow" :class="{ expanded: thinkingExpanded }">
                      <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                        <path d="M4.5 2.5L8 6L4.5 9.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                    </span>
                    <span class="thinking-label">{{ allStepsComplete ? '思考完成' : '思考中...' }}</span>
                    <span v-if="!allStepsComplete" class="thinking-pulse"></span>
                    <span class="thinking-badge">{{ streamingNodes.length }} 步</span>
                  </div>
                  <div class="thinking-timeline-wrapper" :class="{ collapsed: !thinkingExpanded }">
                    <div class="thinking-timeline">
                    <div
                      v-for="(node, idx) in streamingNodes"
                      :key="'step-' + idx"
                      class="timeline-step"
                    >
                      <div class="step-indicator">
                        <div class="step-dot" :class="{ active: node.status === 'processing', done: node.status !== 'processing' }">
                          <svg v-if="node.status !== 'processing'" class="check-icon" width="10" height="10" viewBox="0 0 10 10" fill="none">
                            <path d="M2 5L4.5 7.5L8 3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
                          </svg>
                        </div>
                        <div v-if="idx < streamingNodes.length - 1" class="step-line" :class="{ done: node.status !== 'processing' }"></div>
                      </div>
                      <div class="step-body">
                        <span class="step-name">{{ getStepLabel(node.node) }}</span>
                        <span class="step-desc">{{ node.message }}</span>
                      </div>
                    </div>
                    </div>
                  </div>  <!-- thinking-timeline-wrapper -->
                </div>

                <!-- 流式内容输出 -->
                <div v-if="streamingContent" class="streaming-response">
                  <MarkdownRenderer :content="streamingContent" />
                </div>

                <!-- 初始加载动画 (无节点时) -->
                <div v-if="!streamingContent && streamingNodes.length === 0" class="thinking-dots">
                  <span class="dot"></span>
                  <span class="dot"></span>
                  <span class="dot"></span>
                </div>
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
import { ref, computed, nextTick, onMounted, watch, h } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import { ArrowUpOutlined, UserOutlined, SettingOutlined, LogoutOutlined, ExclamationCircleOutlined, PlusOutlined } from '@ant-design/icons-vue'
import Sidebar from '../components/Sidebar.vue'
import MarkdownRenderer from '../components/MarkdownRenderer.vue'
import { sendChatMessageStream } from '../api/request.js'
import { logout } from '../api/auth.js'
import { getChatHistory, deleteConversation } from '../api/chat.js'

const router = useRouter()
const sidebarCollapsed = ref(false)
const chats = ref([])
const activeChat = ref(null)
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)
const streamingNodes = ref([])
// Each element: { node, status, message, content }
const thinkingExpanded = ref(true)

const userName = ref(localStorage.getItem('username') || '我')
const abortController = ref(null)
const panda = ref("/images/panda.svg")
const AIAvatar = ref("/images/xurx_masaike.png")
const SpringAIAvatar = ref("/images/spring-logo.svg")
const selectedModel = ref('deepseek') // 默认为deepseek
const modelDropdownOpen = ref(false)
const modelList = [
  { value: 'deepseek', name: 'DeepSeek', desc: '深度推理，适合复杂问题' },
  { value: 'qwen', name: 'Qwen', desc: '通义千问，多场景通用' },
  { value: 'zhipu', name: 'Zhipu', desc: '智谱清言，高效对话' },
]
const modelMap = Object.fromEntries(modelList.map(m => [m.value, m]))
const selectModel = (value) => {
  selectedModel.value = value
  modelDropdownOpen.value = false
}
const handleMenuClick = ({ key }) => {
  if (key === 'logout') {
    Modal.confirm({
      title: '确认退出',
      content: '确定要退出登录吗？',
      icon: h(ExclamationCircleOutlined),
      okText: '退出',
      okType: 'danger',
      cancelText: '取消',
      centered: true,
      onOk() {
        logout()
        message.success('已退出登录')
        router.push('/auth')
      }
    })
  } else if (key === 'profile') {
    message.info('个人信息功能开发中')
  } else if (key === 'settings') {
    message.info('设置功能开发中')
  }
}

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

const allStepsComplete = computed(() => {
  return streamingNodes.value.length > 0 &&
    streamingNodes.value.every(n => n.status !== 'processing')
})

const streamingContent = computed(() => {
  const nodes = [...streamingNodes.value]
  const execNode = nodes.reverse().find(n => n.node !== 'intent_recognition' && n.content)
  return execNode ? execNode.content : ''
})

const getStepLabel = (nodeName) => {
  const labels = {
    'intent_recognition': '意图识别',
    'tool_execution': '工具调用',
    'response_generation': '生成回答',
    'database_query': '数据库查询',
    'math_calculation': '数学计算',
  }
  return labels[nodeName] || nodeName
}

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
    async onOk() {
      try {
        await deleteConversation(id)

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
        throw error
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

const loadChatHistoryFromDB = async () => {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    console.log('未找到userId，跳过加载历史记录')
    return
  }

  try {
    const records = await getChatHistory(userId)
    console.log('从数据库加载聊天记录:', records)

    // 按sessionId分组聊天记录
    const sessionMap = new Map()
    records.forEach(record => {
      if (!sessionMap.has(record.sessionId)) {
        sessionMap.set(record.sessionId, [])
      }
      sessionMap.get(record.sessionId).push({
        id: record.id.toString(),
        role: record.role,
        content: record.content,
        time: new Date(record.createTime).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
      })
    })

    // 将分组后的记录转换为chats格式
    const dbChats = []
    sessionMap.forEach((messages, sessionId) => {
      if (messages.length > 0) {
        const firstMessage = messages[0].content
        dbChats.push({
          id: sessionId,
          title: firstMessage.substring(0, 20) + (firstMessage.length > 20 ? '...' : ''),
          messages: messages,
          createdAt: new Date(records.find(r => r.sessionId === sessionId).createTime)
        })
      }
    })

    // 按创建时间排序（数据库为准）
    dbChats.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

    // 使用数据库的结果覆盖本地缓存，防止渲染不存在的记录
    const previousActiveId = activeChat.value
    chats.value = [...dbChats]

    if (previousActiveId && chats.value.some(chat => chat.id === previousActiveId)) {
      activeChat.value = previousActiveId
    } else if (chats.value.length > 0) {
      activeChat.value = chats.value[0].id
    } else {
      activeChat.value = null
    }

    // 保存到localStorage
    saveChats()

    if (dbChats.length > 0) {
      message.success(`已加载 ${dbChats.length} 个历史会话`)
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
    message.error('加载历史记录失败')
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
  streamingNodes.value = []
  thinkingExpanded.value = true
  scrollToBottom()

  // 创建新的 AbortController
  abortController.value = new AbortController()

  try {
    const fullMessage = await sendChatMessageStream(
      userMessage,
      selectedModel.value,
      (eventType, data) => {
        if (eventType === 'step') {
          const idx = streamingNodes.value.findIndex(n => n.node === data.node)
          if (idx >= 0) {
            streamingNodes.value.splice(idx, 1, { ...streamingNodes.value[idx], ...data })
          } else {
            streamingNodes.value.push({ ...data, content: '' })
          }
        } else if (eventType === 'token') {
          const execNode = streamingNodes.value.findLast(n => n.node !== 'intent_recognition')
          if (execNode) execNode.content = data.content
        }
        scrollToBottom()
      },
      abortController.value.signal,
      activeChat.value  // Pass the chat ID as sessionId
    )

    chat.messages.push({
      id: generateId(),
      role: 'assistant',
      content: fullMessage || '抱歉,我没有收到响应',
      time: getCurrentTime()
    })

    streamingNodes.value = []
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
    streamingNodes.value = []
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

// 当流式内容到达且所有步骤完成时，延迟自动折叠思考过程
let collapseTimer = null
watch([streamingContent, allStepsComplete], ([content, done]) => {
  if (collapseTimer) {
    clearTimeout(collapseTimer)
    collapseTimer = null
  }
  if (content && done) {
    collapseTimer = setTimeout(() => {
      thinkingExpanded.value = false
      collapseTimer = null
    }, 800)
  }
})

onMounted(async () => {
  loadChats()

  // 从数据库加载历史记录
  await loadChatHistoryFromDB()
  scrollToBottom()
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
  padding: 10px 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--bg-primary);
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: border-color var(--transition-normal), background var(--transition-normal);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-logo {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.theme-btn:hover {
  color: var(--accent-color);
  background: var(--accent-light);
  transform: rotate(15deg);
}

/* ===== Model Trigger Button ===== */
.model-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  transition: background var(--transition-fast);
  color: var(--text-primary);
}

.model-trigger:hover {
  background: var(--bg-tertiary);
}

.model-trigger-name {
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
}

.model-trigger-arrow {
  color: var(--text-tertiary);
  transition: transform 0.2s ease;
  flex-shrink: 0;
}

.model-trigger-arrow.open {
  transform: rotate(180deg);
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

/* ===== 工作流步骤时间线 UI ===== */
.streaming-card .message-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.thinking-process {
  border-radius: 10px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  overflow: hidden;
  transition: all var(--transition-fast);
}

.thinking-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  cursor: pointer;
  user-select: none;
  transition: background var(--transition-fast);
}

.thinking-header:hover {
  background: color-mix(in srgb, var(--bg-tertiary) 50%, transparent);
}

.thinking-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  color: var(--text-tertiary);
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  flex-shrink: 0;
}

.thinking-arrow.expanded {
  transform: rotate(90deg);
}

.thinking-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
}

.thinking-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-color);
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.8); }
}

.thinking-badge {
  font-size: 11px;
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  padding: 2px 8px;
  border-radius: 10px;
  margin-left: auto;
  font-variant-numeric: tabular-nums;
}

.thinking-timeline-wrapper {
  display: grid;
  grid-template-rows: 1fr;
  transition: grid-template-rows 0.45s cubic-bezier(0.32, 0.72, 0, 1),
              opacity 0.35s ease;
  opacity: 1;
}

.thinking-timeline-wrapper.collapsed {
  grid-template-rows: 0fr;
  opacity: 0;
}

.thinking-timeline {
  overflow: hidden;
  padding: 4px 14px 14px;
  min-height: 0;
}

.timeline-step {
  display: flex;
  gap: 12px;
  min-height: 40px;
  animation: stepSlideIn 0.35s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes stepSlideIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.step-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
  width: 22px;
}

.step-dot {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  background: var(--bg-primary);
  border: 2px solid var(--border-color);
}

.step-dot.active {
  border-color: var(--accent-color);
  background: var(--accent-light);
  animation: dotPulse 2s ease-in-out infinite;
}

.step-dot.active::after {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent-color);
}

.step-dot.done {
  border-color: var(--success-color);
  background: var(--success-color);
  color: white;
}

.check-icon {
  width: 10px;
  height: 10px;
}

@keyframes dotPulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.35);
  }
  50% {
    box-shadow: 0 0 0 6px rgba(37, 99, 235, 0);
  }
}

.step-line {
  width: 2px;
  flex: 1;
  min-height: 12px;
  background: var(--border-color);
  margin: 3px 0;
  border-radius: 1px;
  transition: background 0.4s ease;
}

.step-line.done {
  background: var(--success-color);
}

.step-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 2px 0 10px;
  min-width: 0;
}

.step-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
}

.step-desc {
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.4;
  word-break: break-word;
}

.streaming-response {
  animation: contentFadeIn 0.3s ease;
}

@keyframes contentFadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 三点加载动画 */
.thinking-dots {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 8px 0;
}

.thinking-dots .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-tertiary);
  animation: dotBounce 1.4s ease-in-out infinite both;
}

.thinking-dots .dot:nth-child(1) { animation-delay: 0s; }
.thinking-dots .dot:nth-child(2) { animation-delay: 0.16s; }
.thinking-dots .dot:nth-child(3) { animation-delay: 0.32s; }

@keyframes dotBounce {
  0%, 80%, 100% {
    opacity: 0.3;
    transform: scale(0.7);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
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

<style>
/* ===== Model Dropdown Popover (global, popover renders outside scoped) ===== */
.model-dropdown-popover .ant-popover-inner {
  padding: 0 !important;
  border-radius: 14px !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06) !important;
  border: 1px solid var(--border-color) !important;
  background: var(--bg-primary) !important;
  overflow: hidden;
}

.model-dropdown-panel {
  min-width: 240px;
  padding: 6px 0;
}

.model-dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s ease;
  gap: 16px;
}

.model-dropdown-item:hover {
  background: var(--bg-secondary);
}

.model-dropdown-item.selected {
  background: transparent;
}

.model-item-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.model-item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  line-height: 1.4;
}

.model-item-desc {
  font-size: 12px;
  color: var(--text-tertiary);
  line-height: 1.4;
}

.model-item-check {
  color: var(--text-primary);
  flex-shrink: 0;
}

[data-theme="dark"] .model-dropdown-popover .ant-popover-inner {
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4), 0 2px 8px rgba(0, 0, 0, 0.2) !important;
}
</style>
