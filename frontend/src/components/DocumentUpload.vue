<template>
  <div class="document-upload-container">
    <!-- 上传区域 -->
    <div class="upload-section">
      <div class="upload-area-wrapper">
        <a-upload-dragger
          v-model:file-list="fileList"
          name="file"
          :multiple="false"
          :action="uploadUrl"
          :headers="uploadHeaders"
          :data="uploadData"
          :before-upload="beforeUpload"
          @change="handleUploadChange"
          accept=".pdf,.doc,.docx,.ppt,.pptx,.txt,.md,.html"
          class="upload-dragger"
        >
          <div class="upload-content">
            <div class="upload-icon-wrapper">
              <inbox-outlined class="upload-icon" />
            </div>
            <div class="upload-text">
              <p class="upload-title">点击或拖拽文件到此区域上传</p>
              <p class="upload-hint">支持 PDF、Word、PPT、TXT、Markdown、HTML 格式，单文件最大 50MB</p>
            </div>
          </div>
        </a-upload-dragger>
      </div>

      <!-- 上传配置 -->
      <div class="upload-config">
        <div class="config-header">
          <span class="config-title">处理配置</span>
        </div>
        <div class="config-grid">
          <div class="config-item">
            <div class="config-label">
              <label>分块大小</label>
              <a-tooltip title="将文档切分成多个片段，每个片段的最大token数">
                <question-circle-outlined class="help-icon" />
              </a-tooltip>
            </div>
            <a-input-number
              v-model:value="chunkSize"
              :min="MIN_CHUNK_SIZE"
              :max="MAX_CHUNK_SIZE"
              :step="100"
              class="config-input"
              addon-after="tokens"
            />
            <span class="config-hint">推荐 800</span>
          </div>
          <div class="config-item">
            <div class="config-label">
              <label>分块重叠</label>
              <a-tooltip title="相邻片段之间重叠的token数，保持上下文连贯性">
                <question-circle-outlined class="help-icon" />
              </a-tooltip>
            </div>
            <a-input-number
              v-model:value="chunkOverlap"
              :min="MIN_CHUNK_OVERLAP"
              :max="overlapUpperBound"
              :step="50"
              class="config-input"
              addon-after="tokens"
            />
            <span class="config-hint">推荐 200</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 文档列表 -->
    <div class="document-list-section">
      <div class="list-header">
        <div class="header-left">
          <h3>已上传文档</h3>
          <span class="doc-count">{{ documentList.length }} 个文档</span>
        </div>
        <a-button @click="loadDocumentList" :loading="loading" type="text" class="refresh-btn">
          <template #icon><reload-outlined /></template>
          刷新
        </a-button>
      </div>

      <a-spin :spinning="loading">
        <div v-if="documentList.length > 0" class="document-list">
          <div
            v-for="item in documentList"
            :key="item.fileName"
            class="document-item"
          >
            <div class="item-icon">
              <component :is="getFileIcon(item.fileName)" />
            </div>
            <div class="item-info">
              <div class="file-name" :title="item.fileName">{{ item.fileName }}</div>
              <div class="file-meta">
                <span>{{ formatFileSize(item.fileSize) }}</span>
                <span class="meta-divider"></span>
                <span>{{ formatTime(item.uploadTime) }}</span>
              </div>
            </div>
            <div class="item-actions">
              <a-popconfirm
                title="确定要删除这个文档吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="deleteDocument(item.fileName)"
              >
                <a-button type="text" size="small" class="delete-btn">
                  <template #icon><delete-outlined /></template>
                </a-button>
              </a-popconfirm>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <inbox-outlined class="empty-icon" />
          <p class="empty-title">暂无文档</p>
          <p class="empty-hint">上传文档以构建知识库</p>
        </div>
      </a-spin>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  InboxOutlined,
  ReloadOutlined,
  FileTextOutlined,
  DeleteOutlined,
  QuestionCircleOutlined,
  FilePdfOutlined,
  FileWordOutlined,
  FilePptOutlined,
  FileMarkdownOutlined
} from '@ant-design/icons-vue'

// 根据文件名获取文件类型图标
const getFileIcon = (fileName) => {
  const ext = fileName.split('.').pop().toLowerCase()
  const iconMap = {
    pdf: FilePdfOutlined,
    doc: FileWordOutlined,
    docx: FileWordOutlined,
    ppt: FilePptOutlined,
    pptx: FilePptOutlined,
    md: FileMarkdownOutlined,
    txt: FileTextOutlined,
    html: FileTextOutlined
  }
  return iconMap[ext] || FileTextOutlined
}

// Props
const props = defineProps({
  userId: {
    type: String,
    required: true
  }
})

// 状态
const fileList = ref([])
const documentList = ref([])
const loading = ref(false)

const MIN_CHUNK_SIZE = 100
const MAX_CHUNK_SIZE = 2000
const MIN_CHUNK_OVERLAP = 0
const MAX_CHUNK_OVERLAP = 500
const DEFAULT_CHUNK_SIZE = 800
const DEFAULT_CHUNK_OVERLAP = 200

const chunkSize = ref(DEFAULT_CHUNK_SIZE)
const chunkOverlap = ref(DEFAULT_CHUNK_OVERLAP)

// API Base URL
const API_BASE = 'http://localhost:8080'

// 上传配置
const uploadUrl = computed(() => {
  const params = new URLSearchParams({
    userId: props.userId,
    chunkSize: `${chunkSize.value}`,
    chunkOverlap: `${chunkOverlap.value}`
  })
  return `${API_BASE}/api/document/upload?${params.toString()}`
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return {
    'Authorization': token ? `Bearer ${token}` : ''
  }
})

const uploadData = computed(() => ({
  userId: props.userId,
  chunkSize: chunkSize.value,
  chunkOverlap: chunkOverlap.value
}))

const overlapUpperBound = computed(() => {
  const dynamicUpperBound = chunkSize.value - 1
  const safeUpperBound = Math.max(MIN_CHUNK_OVERLAP, dynamicUpperBound)
  return Math.min(MAX_CHUNK_OVERLAP, safeUpperBound)
})

const clampNumber = (value, min, max) => {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return min
  }
  return Math.min(Math.max(value, min), max)
}

const enforceOverlapRange = () => {
  if (chunkOverlap.value > overlapUpperBound.value) {
    chunkOverlap.value = overlapUpperBound.value
  }
}

watch(chunkSize, (newValue) => {
  const normalized = clampNumber(newValue, MIN_CHUNK_SIZE, MAX_CHUNK_SIZE)
  if (normalized !== newValue) {
    chunkSize.value = normalized
    return
  }
  enforceOverlapRange()
})

watch(chunkOverlap, (newValue) => {
  const normalized = clampNumber(newValue, MIN_CHUNK_OVERLAP, overlapUpperBound.value)
  if (normalized !== newValue) {
    chunkOverlap.value = normalized
  }
})

// 上传前验证
const beforeUpload = (file) => {
  const validTypes = [
    'application/pdf',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.presentationml.presentation',
    'application/vnd.ms-powerpoint',
    'text/plain',
    'text/markdown',
    'text/html'
  ]

  const isValidType = validTypes.includes(file.type) ||
                      file.name.endsWith('.md') ||
                      file.name.endsWith('.txt')

  if (!isValidType) {
    message.error('不支持的文件格式，仅支持 PDF、Word、PPT、TXT、Markdown、HTML')
    return false
  }

  const isLt50M = file.size / 1024 / 1024 < 50
  if (!isLt50M) {
    message.error('文件大小不能超过 50MB')
    return false
  }

  return true
}

// 上传状态变化
const handleUploadChange = (info) => {
  const { status, response } = info.file

  if (status === 'uploading') {
    message.loading({ content: '正在上传并处理文档...', key: 'upload', duration: 0 })
  }

  if (status === 'done') {
    message.destroy('upload')
    if (response && response.success) {
      message.success(`${info.file.name} 上传成功，已分块 ${response.chunkCount} 个片段`)
      fileList.value = []
      loadDocumentList()
    } else {
      message.error(`${info.file.name} 上传失败：${response?.error || '未知错误'}`)
    }
  } else if (status === 'error') {
    message.destroy('upload')
    message.error(`${info.file.name} 上传失败`)
  }
}

// 加载文档列表
const loadDocumentList = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`${API_BASE}/api/document/list?userId=${props.userId}`, {
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    })

    const data = await response.json()

    if (data.success) {
      documentList.value = data.documents || []
    } else {
      message.error('获取文档列表失败')
    }
  } catch (error) {
    console.error('获取文档列表失败:', error)
    message.error('获取文档列表失败')
  } finally {
    loading.value = false
  }
}

// 删除文档
const deleteDocument = async (fileName) => {
  try {
    const token = localStorage.getItem('token')
    const response = await fetch(`${API_BASE}/api/document/delete?userId=${props.userId}&fileName=${encodeURIComponent(fileName)}`, {
      method: 'DELETE',
      headers: {
        'Authorization': token ? `Bearer ${token}` : ''
      }
    })

    const data = await response.json()

    if (data.success) {
      message.success('文档删除成功')
      loadDocumentList()
    } else {
      message.error('文档删除失败')
    }
  } catch (error) {
    console.error('删除文档失败:', error)
    message.error('删除文档失败')
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  try {
    const date = new Date(timeStr)
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return timeStr
  }
}

// 组件挂载时加载文档列表
onMounted(() => {
  loadDocumentList()
})
</script>

<style scoped>
.document-upload-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

/* 上传区域 */
.upload-section {
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  padding: 24px;
  margin-bottom: 24px;
  border: 1px solid var(--border-color);
}

.upload-dragger {
  margin-bottom: 20px;
}

.upload-content {
  padding: 32px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.upload-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-icon {
  font-size: 48px;
  color: var(--text-tertiary);
}

.upload-text {
  text-align: center;
}

.upload-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.upload-hint {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

/* 上传配置 */
.upload-config {
  padding: 16px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
}

.config-header {
  margin-bottom: 16px;
}

.config-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.config-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.config-label {
  display: flex;
  align-items: center;
  gap: 6px;
}

.config-label label {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.help-icon {
  font-size: 12px;
  color: var(--text-tertiary);
  cursor: help;
}

.config-input {
  width: 100%;
}

.config-hint {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* 文档列表 */
.document-list-section {
  background: var(--bg-primary);
  border-radius: var(--radius-md);
  padding: 24px;
  border: 1px solid var(--border-color);
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--border-color);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
}

.doc-count {
  font-size: 13px;
  color: var(--text-secondary);
}

.refresh-btn {
  color: var(--text-secondary);
}

.refresh-btn:hover {
  color: var(--accent-color);
}

/* 文档列表 */
.document-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.document-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  transition: all var(--transition-fast);
}

.document-item:hover {
  border-color: var(--accent-color);
  background: var(--bg-primary);
}

.item-icon {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-tertiary);
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: 18px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
}

.file-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.meta-divider {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--text-tertiary);
}

.item-actions {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity var(--transition-fast);
}

.document-item:hover .item-actions {
  opacity: 1;
}

.delete-btn {
  color: var(--text-tertiary);
}

.delete-btn:hover {
  color: var(--error-color);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 48px 20px;
}

.empty-icon {
  font-size: 48px;
  color: var(--text-tertiary);
  margin-bottom: 12px;
}

.empty-title {
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
  margin: 0 0 4px 0;
}

.empty-hint {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0;
}

/* Ant Design 上传组件样式覆盖 */
:deep(.ant-upload.ant-upload-drag) {
  background: var(--bg-secondary);
  border: 1px dashed var(--border-color);
  border-radius: var(--radius-sm);
  transition: all var(--transition-fast);
}

:deep(.ant-upload.ant-upload-drag:hover) {
  border-color: var(--accent-color);
}

:deep(.ant-upload.ant-upload-drag.ant-upload-drag-hover) {
  border-color: var(--accent-color);
  background: var(--accent-light);
}

:deep(.ant-upload-list) {
  display: none !important;
}

/* Ant Design 输入框样式覆盖 */
:deep(.ant-input-number) {
  background: var(--bg-primary);
  border-color: var(--border-color);
}

:deep(.ant-input-number:hover),
:deep(.ant-input-number:focus),
:deep(.ant-input-number-focused) {
  border-color: var(--accent-color);
}

:deep(.ant-input-number-input) {
  color: var(--text-primary);
}

:deep(.ant-input-number-group-addon) {
  background: var(--bg-tertiary);
  border-color: var(--border-color);
  color: var(--text-secondary);
}

/* 深色模式适配 */
[data-theme="dark"] .upload-section,
[data-theme="dark"] .document-list-section {
  background: var(--bg-primary);
  border-color: var(--border-color);
}

[data-theme="dark"] .upload-config {
  background: var(--bg-secondary);
  border-color: var(--border-color);
}

[data-theme="dark"] .document-item {
  background: var(--bg-secondary);
  border-color: var(--border-color);
}

[data-theme="dark"] .document-item:hover {
  background: var(--bg-tertiary);
  border-color: var(--accent-color);
}

[data-theme="dark"] .item-icon {
  background: var(--bg-tertiary);
}

[data-theme="dark"] :deep(.ant-upload.ant-upload-drag) {
  background: var(--bg-secondary);
  border-color: var(--border-color);
}

[data-theme="dark"] :deep(.ant-upload.ant-upload-drag:hover) {
  border-color: var(--accent-color);
}

[data-theme="dark"] :deep(.ant-upload.ant-upload-drag.ant-upload-drag-hover) {
  background: var(--bg-tertiary);
}

[data-theme="dark"] :deep(.ant-input-number) {
  background: var(--bg-primary);
  border-color: var(--border-color);
}

[data-theme="dark"] :deep(.ant-input-number-input) {
  color: var(--text-primary);
}

[data-theme="dark"] :deep(.ant-input-number-group-addon) {
  background: var(--bg-tertiary);
  border-color: var(--border-color);
  color: var(--text-secondary);
}

[data-theme="dark"] :deep(.ant-input-number-handler-wrap) {
  background: var(--bg-secondary);
}

[data-theme="dark"] :deep(.ant-input-number-handler) {
  border-color: var(--border-color);
}

[data-theme="dark"] :deep(.ant-input-number-handler-up-inner),
[data-theme="dark"] :deep(.ant-input-number-handler-down-inner) {
  color: var(--text-secondary);
}

[data-theme="dark"] :deep(.ant-popconfirm-inner-content) {
  color: var(--text-primary);
}

/* 响应式设计 */
@media (max-width: 640px) {
  .document-upload-container {
    padding: 16px;
  }

  .upload-section,
  .document-list-section {
    padding: 16px;
  }

  .upload-content {
    padding: 24px 16px;
  }

  .config-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .document-item {
    padding: 10px;
  }

  .item-actions {
    opacity: 1;
  }
}
</style>
