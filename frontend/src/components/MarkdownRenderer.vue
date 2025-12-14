<template>
  <div class="markdown-content" v-html="renderedHtml" ref="contentRef"></div>
</template>

<script setup>
import { computed, ref, onMounted, watch, nextTick } from 'vue'
import { marked } from 'marked'
import hljs from 'highlight.js'
import { message } from 'ant-design-vue'

const props = defineProps({
  content: {
    type: String,
    required: true
  }
})

const contentRef = ref(null)

marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (err) {
        console.error(err)
      }
    }
    return hljs.highlightAuto(code).value
  },
  breaks: true,
  gfm: true
})

const renderedHtml = computed(() => {
  return marked.parse(props.content || '')
})

const addCopyButtons = () => {
  if (!contentRef.value) return

  const codeBlocks = contentRef.value.querySelectorAll('pre')
  codeBlocks.forEach((pre) => {
    // 避免重复添加按钮
    if (pre.querySelector('.copy-button')) return

    const button = document.createElement('button')
    button.className = 'copy-button'
    button.innerHTML = `
      <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none">
        <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
        <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
      </svg>
    `
    button.setAttribute('aria-label', '复制代码')

    button.addEventListener('click', async () => {
      const code = pre.querySelector('code')
      if (code) {
        try {
          await navigator.clipboard.writeText(code.textContent || '')
          button.innerHTML = `
            <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          `
          message.success('代码已复制')
          setTimeout(() => {
            button.innerHTML = `
              <svg viewBox="0 0 24 24" width="16" height="16" stroke="currentColor" stroke-width="2" fill="none">
                <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
              </svg>
            `
          }, 2000)
        } catch (err) {
          message.error('复制失败')
        }
      }
    })

    pre.style.position = 'relative'
    pre.appendChild(button)
  })
}

watch(() => props.content, () => {
  nextTick(() => {
    addCopyButtons()
  })
})

onMounted(() => {
  addCopyButtons()
})
</script>

<style scoped>
.markdown-content {
  line-height: 1.6;
  color: var(--text-primary);
}

.markdown-content :deep(h1),
.markdown-content :deep(h2),
.markdown-content :deep(h3),
.markdown-content :deep(h4),
.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  margin-top: 20px;
  margin-bottom: 12px;
  font-weight: 600;
  line-height: 1.3;
  color: var(--text-primary);
  transition: color var(--transition-normal);
}

.markdown-content :deep(h1) { font-size: 1.5em; }
.markdown-content :deep(h2) { font-size: 1.3em; }
.markdown-content :deep(h3) { font-size: 1.15em; }

.markdown-content :deep(p) {
  margin-bottom: 12px;
}

.markdown-content :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(code) {
  background: var(--code-bg);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'SF Mono', 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.875em;
  color: var(--text-primary);
  transition: background var(--transition-normal), color var(--transition-normal);
}

.markdown-content :deep(pre) {
  background: var(--code-block-bg);
  padding: 16px;
  border-radius: var(--radius-md);
  overflow-x: auto;
  margin: 12px 0;
  position: relative;
  border: 1px solid var(--border-color);
  transition: background var(--transition-normal), border-color var(--transition-normal);
}

[data-theme="dark"] .markdown-content :deep(pre) {
  border: none;
}

.markdown-content :deep(.copy-button) {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 6px 8px;
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  color: var(--text-primary);
  cursor: pointer;
  opacity: 0;
  transition: all var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8px);
}

[data-theme="dark"] .markdown-content :deep(.copy-button) {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: var(--code-block-text);
}

.markdown-content :deep(pre:hover .copy-button) {
  opacity: 1;
}

.markdown-content :deep(.copy-button:hover) {
  background: rgba(0, 0, 0, 0.1);
  border-color: rgba(0, 0, 0, 0.2);
  transform: scale(1.05);
}

[data-theme="dark"] .markdown-content :deep(.copy-button:hover) {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
}

.markdown-content :deep(.copy-button:active) {
  transform: scale(0.95);
}

.markdown-content :deep(pre code) {
  background: transparent;
  padding: 0;
  color: var(--code-block-text);
  font-size: 13px;
  line-height: 1.5;
  transition: color var(--transition-normal);
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin-bottom: 12px;
  padding-left: 20px;
}

.markdown-content :deep(li) {
  margin-bottom: 6px;
}

.markdown-content :deep(li:last-child) {
  margin-bottom: 0;
}

.markdown-content :deep(blockquote) {
  border-left: 3px solid var(--border-color);
  padding-left: 14px;
  margin: 12px 0;
  color: var(--text-secondary);
  transition: border-color var(--transition-normal), color var(--transition-normal);
}

.markdown-content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
  font-size: 13px;
  transition: border-color var(--transition-normal);
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  border: 1px solid var(--border-color);
  padding: 8px 12px;
  text-align: left;
  transition: border-color var(--transition-normal), background var(--transition-normal);
}

.markdown-content :deep(th) {
  background: var(--bg-tertiary);
  font-weight: 600;
}

.markdown-content :deep(a) {
  color: var(--accent-color);
  text-decoration: none;
  transition: color var(--transition-normal);
}

.markdown-content :deep(a:hover) {
  text-decoration: underline;
  color: var(--accent-hover);
}

.markdown-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-md);
  margin: 12px 0;
}

.markdown-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--border-color);
  margin: 20px 0;
  transition: border-color var(--transition-normal);
}

.markdown-content :deep(strong) {
  font-weight: 600;
}

/* Syntax highlighting - 使用主题变量 */
.markdown-content :deep(.hljs-keyword),
.markdown-content :deep(.hljs-selector-tag),
.markdown-content :deep(.hljs-built_in) {
  color: var(--syntax-keyword);
}

.markdown-content :deep(.hljs-string),
.markdown-content :deep(.hljs-attr) {
  color: var(--syntax-string);
}

.markdown-content :deep(.hljs-number),
.markdown-content :deep(.hljs-literal) {
  color: var(--syntax-number);
}

.markdown-content :deep(.hljs-function),
.markdown-content :deep(.hljs-title) {
  color: var(--syntax-function);
}

.markdown-content :deep(.hljs-comment) {
  color: var(--syntax-comment);
  font-style: italic;
}

.markdown-content :deep(.hljs-variable),
.markdown-content :deep(.hljs-params) {
  color: var(--syntax-variable);
}

.markdown-content :deep(.hljs-type),
.markdown-content :deep(.hljs-class) {
  color: var(--syntax-type);
}
</style>
