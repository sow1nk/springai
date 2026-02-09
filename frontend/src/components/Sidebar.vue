<template>
  <div class="sidebar" :class="{ collapsed: collapsed }" role="navigation" aria-label="侧边栏导航">
    <div class="sidebar-header">
      <span v-if="!collapsed" class="logo">Spring AI</span>
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
        <!-- <PlusOutlined /> -->
         <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 0.599609C3.91309 0.599609 0.599609 3.91309 0.599609 8C0.599609 9.13376 0.855461 10.2098 1.3125 11.1719L1.5918 11.7588L2.76562 11.2012L2.48633 10.6143C2.11034 9.82278 1.90039 8.93675 1.90039 8C1.90039 4.63106 4.63106 1.90039 8 1.90039C11.3689 1.90039 14.0996 4.63106 14.0996 8C14.0996 11.3689 11.3689 14.0996 8 14.0996C7.31041 14.0996 6.80528 14.0514 6.35742 13.9277C5.91623 13.8059 5.49768 13.6021 4.99707 13.2529C4.26492 12.7422 3.21611 12.5616 2.35156 13.1074L2.33789 13.1162L2.32422 13.126L1.58789 13.6436L2.01953 14.9297L3.0459 14.207C3.36351 14.0065 3.83838 14.0294 4.25293 14.3184C4.84547 14.7317 5.39743 15.011 6.01172 15.1807C6.61947 15.3485 7.25549 15.4004 8 15.4004C12.0869 15.4004 15.4004 12.0869 15.4004 8C15.4004 3.91309 12.0869 0.599609 8 0.599609ZM7.34473 4.93945V7.34961H4.93945V8.65039H7.34473V11.0605H8.64551V8.65039H11.0605V7.34961H8.64551V4.93945H7.34473Z" fill="currentColor"></path></svg>
        <span>开启新对话</span>
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
        class="theme-toggle"
        @click="toggleTheme"
        :aria-label="isDark ? '切换到浅色模式' : '切换到深色模式'"
      >
        <BulbOutlined aria-hidden="true" />
        <span>{{ isDark ? '浅色模式' : '深色模式' }}</span>
        <span class="theme-switch" :class="{ on: isDark }">
          <span class="switch-dot"></span>
        </span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  PlusOutlined,
  MessageOutlined,
  DeleteOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
  BulbOutlined
} from '@ant-design/icons-vue'

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
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 56px;
}

.logo {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.toggle-btn {
  color: var(--text-tertiary);
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  transition: background var(--transition-fast), color var(--transition-fast);
  font-size: 13px;
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
  justify-content: center;
  gap: 6px;
  padding: 9px 12px;
  margin-bottom: 16px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-primary);
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: background var(--transition-fast), border-color var(--transition-fast);
}

.new-chat-btn:hover {
  background: var(--bg-tertiary);
  border-color: var(--text-tertiary);
}

.new-chat-btn:active {
  opacity: 0.85;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.chat-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 8px;
  cursor: pointer;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
  position: relative;
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

.chat-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 14px;
  border-radius: 0 2px 2px 0;
  background: var(--accent-color);
}

.chat-icon {
  font-size: 13px;
  flex-shrink: 0;
  opacity: 0.5;
}

.chat-item.active .chat-icon {
  opacity: 1;
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
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
  flex-shrink: 0;
  min-width: 22px;
  min-height: 22px;
  font-size: 12px;
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
  padding: 10px 12px;
  border-top: 1px solid var(--border-light);
  transition: border-color var(--transition-normal);
}

.theme-toggle {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 12px;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.theme-toggle:hover {
  background: var(--bg-tertiary);
  color: var(--text-secondary);
}

.theme-switch {
  width: 28px;
  height: 16px;
  border-radius: 8px;
  background: var(--border-color);
  margin-left: auto;
  position: relative;
  transition: background 0.25s ease;
  flex-shrink: 0;
}

.theme-switch.on {
  background: var(--accent-color);
}

.switch-dot {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: white;
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.theme-switch.on .switch-dot {
  transform: translateX(12px);
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
