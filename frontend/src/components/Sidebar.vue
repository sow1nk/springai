<template>
  <div class="sidebar" :class="{ collapsed: collapsed }" role="navigation" aria-label="侧边栏导航">
    <div class="sidebar-header">
      <span v-if="!collapsed" class="logo">AI</span>
      <a-button
        type="text"
        @click="toggleSidebar"
        class="toggle-btn"
        :aria-label="collapsed ? '展开侧边栏' : '收起侧边栏'"
      >
        <template #icon>
          <MenuFoldOutlined v-if="!collapsed" />
          <MenuUnfoldOutlined v-else />
        </template>
      </a-button>
    </div>

    <div v-if="!collapsed" class="sidebar-content">
      <button class="new-chat-btn" @click="createNewChat" aria-label="创建新对话">
        <PlusOutlined />
        <span>新对话</span>
      </button>

      <div class="chat-list" role="list" aria-label="对话列表">
        <div
          v-for="chat in chats"
          :key="chat.id"
          :class="['chat-item', { active: chat.id === activeId }]"
          @click="selectChat(chat.id)"
          role="listitem"
          :aria-label="`对话: ${chat.title}`"
          :aria-current="chat.id === activeId ? 'true' : 'false'"
          tabindex="0"
          @keydown.enter="selectChat(chat.id)"
          @keydown.space.prevent="selectChat(chat.id)"
        >
          <MessageOutlined class="chat-icon" aria-hidden="true" />
          <span class="chat-title">{{ chat.title }}</span>
          <button
            class="delete-btn"
            @click.stop="deleteChat(chat.id)"
            :aria-label="`删除对话: ${chat.title}`"
            tabindex="0"
          >
            <DeleteOutlined aria-hidden="true" />
          </button>
        </div>
      </div>
    </div>

    <div v-if="!collapsed" class="sidebar-footer">
      <button
        class="knowledge-base-btn"
        @click="goToDocuments"
        aria-label="知识库管理"
      >
        <FolderOpenOutlined aria-hidden="true" />
        <span>知识库</span>
      </button>
      <button
        class="theme-toggle"
        @click="toggleTheme"
        :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'"
      >
        <BulbOutlined aria-hidden="true" />
        <span>{{ isDark ? '浅色模式' : '深色模式' }}</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  PlusOutlined,
  MessageOutlined,
  DeleteOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BulbOutlined,
  FolderOpenOutlined
} from '@ant-design/icons-vue'

const router = useRouter()

defineProps({
  collapsed: Boolean,
  chats: Array,
  activeId: String
})

const emit = defineEmits(['toggle', 'new-chat', 'select-chat', 'delete-chat'])

const isDark = ref(false)

onMounted(() => {
  const savedTheme = localStorage.getItem('springai-theme')
  if (savedTheme) {
    isDark.value = savedTheme === 'dark'
    document.documentElement.setAttribute('data-theme', savedTheme)
  } else {
    isDark.value = document.documentElement.getAttribute('data-theme') === 'dark'
  }
})

const toggleTheme = () => {
  const newIsDark = !isDark.value
  const theme = newIsDark ? 'dark' : 'light'

  // 使用 View Transitions API（如果支持）实现更丝滑的切换
  if (document.startViewTransition) {
    document.startViewTransition(() => {
      isDark.value = newIsDark
      document.documentElement.setAttribute('data-theme', theme)
      localStorage.setItem('springai-theme', theme)
    })
  } else {
    // 回退方案：添加过渡类
    document.documentElement.classList.add('theme-transitioning')
    isDark.value = newIsDark
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('springai-theme', theme)

    // 过渡结束后移除类
    setTimeout(() => {
      document.documentElement.classList.remove('theme-transitioning')
    }, 400)
  }
}

const toggleSidebar = () => {
  emit('toggle')
}

const createNewChat = () => {
  emit('new-chat')
}

const selectChat = (id) => {
  emit('select-chat', id)
}

const deleteChat = (id) => {
  console.log('删除对话被触发:', id)
  emit('delete-chat', id)
}

const goToDocuments = () => {
  router.push('/documents')
}
</script>

<style scoped>
.sidebar {
  width: 260px;
  height: 100vh;
  background: var(--sidebar-bg);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  transition: width var(--transition-normal), background var(--transition-normal), border-color var(--transition-normal);
}

.sidebar.collapsed {
  width: 60px;
}

.sidebar-header {
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 60px;
  transition: border-color var(--transition-normal);
}

.logo {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.5px;
}

.toggle-btn {
  color: var(--text-secondary);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  transition: background var(--transition-fast), color var(--transition-fast);
}

.toggle-btn:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.sidebar.collapsed .sidebar-header {
  justify-content: center;
}

.sidebar-content {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  margin-bottom: 12px;
  border: 1px dashed var(--border-color);
  border-radius: var(--radius-md);
  background: transparent;
  color: var(--text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.new-chat-btn:hover {
  border-color: var(--accent-color);
  color: var(--accent-color);
  background: var(--accent-light);
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.chat-item:hover {
  background: var(--bg-tertiary);
  color: var(--text-primary);
}

.chat-item:focus {
  outline: 2px solid var(--accent-color);
  outline-offset: -2px;
}

.chat-item.active {
  background: var(--accent-light);
  color: var(--accent-color);
}

.chat-icon {
  font-size: 14px;
  flex-shrink: 0;
  opacity: 0.7;
}

.chat-title {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delete-btn {
  opacity: 0;
  padding: 4px;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  flex-shrink: 0;
  min-width: 24px;
  min-height: 24px;
}

.chat-item:hover .delete-btn,
.chat-item:focus .delete-btn,
.chat-item:focus-within .delete-btn {
  opacity: 1;
}

.delete-btn:hover {
  color: var(--error-color);
  background: rgba(220, 38, 38, 0.1);
}

.delete-btn:focus {
  outline: 2px solid var(--error-color);
  outline-offset: 2px;
  opacity: 1;
}

.delete-btn:active {
  transform: scale(0.9);
}

/* 移动端和触摸设备始终显示删除按钮 */
@media (max-width: 768px) {
  .delete-btn {
    opacity: 1;
  }
}

@media (hover: none) {
  .delete-btn {
    opacity: 1;
  }
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid var(--border-color);
  transition: border-color var(--transition-normal);
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.knowledge-base-btn {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--accent-light);
  color: var(--accent-color);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-fast);
  font-weight: 500;
}

.knowledge-base-btn:hover {
  background: var(--accent-color);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.theme-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.theme-toggle:hover {
  color: var(--text-primary);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    left: 0;
    top: 0;
    z-index: 1000;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  }

  .sidebar.collapsed {
    transform: translateX(-100%);
    width: 260px;
  }
}
</style>
