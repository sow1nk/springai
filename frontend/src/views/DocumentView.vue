<template>
  <div class="document-view">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-wrapper">
        <a-button type="text" @click="goBack" class="back-button">
          <template #icon><arrow-left-outlined /></template>
          返回
        </a-button>
        <h1 class="page-title">文档管理</h1>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="page-content">
      <!-- 文档上传组件 -->
      <DocumentUpload v-if="userId" :userId="userId" />

      <!-- 说明信息 -->
      <div class="info-section">
        <div class="info-card">
          <h3 class="info-title">使用说明</h3>
          <ul class="info-list">
            <li>上传的文档会自动分块并存储到向量数据库</li>
            <li>在聊天时，系统会自动检索相关文档内容</li>
            <li>支持 PDF、Word、PPT、TXT、Markdown、HTML 格式</li>
            <li>每位用户拥有独立的文档存储空间</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import DocumentUpload from '../components/DocumentUpload.vue'

const router = useRouter()
const userId = ref('')

// 返回聊天页面
const goBack = () => {
  router.push('/')
}

onMounted(() => {
  // 获取用户ID
  const storedUserId = localStorage.getItem('userId')
  const token = localStorage.getItem('token')

  if (!storedUserId || !token) {
    // 如果未登录，跳转到登录页
    router.push('/auth')
    return
  }

  userId.value = storedUserId
})
</script>

<style scoped>
.document-view {
  min-height: 100vh;
  background: var(--bg-secondary);
}

/* 页面头部 */
.page-header {
  background: var(--bg-primary);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-wrapper {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
}

.back-button {
  font-size: 14px;
  color: var(--text-secondary);
  padding: 4px 8px;
  height: 32px;
}

.back-button:hover {
  color: var(--accent-color);
  background: var(--accent-light);
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

/* 主要内容 */
.page-content {
  max-width: 800px;
  margin: 0 auto;
  padding: 0;
}

/* 说明信息 */
.info-section {
  padding: 0 24px 24px;
}

.info-card {
  background: var(--bg-primary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 20px;
}

.info-title {
  margin: 0 0 12px 0;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.info-list {
  margin: 0;
  padding: 0 0 0 20px;
  list-style: disc;
}

.info-list li {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.8;
}

/* 深色模式 */
[data-theme="dark"] .document-view {
  background: var(--bg-secondary);
}

[data-theme="dark"] .page-header {
  background: var(--bg-primary);
  border-bottom-color: var(--border-color);
}

[data-theme="dark"] .info-card {
  background: var(--bg-primary);
  border-color: var(--border-color);
}

/* 响应式设计 */
@media (max-width: 640px) {
  .header-wrapper {
    padding: 12px 16px;
  }

  .page-title {
    font-size: 16px;
  }

  .info-section {
    padding: 0 16px 16px;
  }

  .info-card {
    padding: 16px;
  }
}
</style>
