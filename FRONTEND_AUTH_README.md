# 前端登录认证功能说明

## 功能概述

本次更新实现了以下前端功能：
1. 简约风格的登录注册界面
2. 深色/浅色主题切换
3. 退出登录功能
4. 路由守卫（自动跳转）

## 功能详情

### 1. 登录注册界面 (AuthView.vue)

**设计特点**：
- 简约设计，与对话界面风格一致
- 使用系统主题变量，无AI配色
- 支持深色/浅色主题自动切换
- 响应式设计，适配移动端

**功能**：
- Tab切换：登录/注册
- 登录：只需输入用户名
- 注册：输入用户名和邮箱（可选）
- 表单验证
- 加载状态显示
- 主题切换按钮（右上角）

**路由**：`/auth`

### 2. 主题切换功能

**位置**：
- 登录页面：右上角主题切换按钮
- 对话页面：右上角主题切换按钮（用户头像左侧）

**实现**：
- 使用CSS变量实现主题切换
- 主题状态保存在localStorage
- 平滑过渡动画
- 图标动态切换（太阳/月亮）

**使用**：
```javascript
// 切换主题
const toggleTheme = () => {
  const newTheme = currentTheme.value === 'light' ? 'dark' : 'light'
  currentTheme.value = newTheme
  document.documentElement.setAttribute('data-theme', newTheme)
  localStorage.setItem('springai-theme', newTheme)
}
```

### 3. 退出登录功能

**位置**：对话页面右上角用户头像下拉菜单

**功能**：
- 点击"退出登录"显示确认对话框
- 确认后清除本地存储的认证信息
- 自动跳转到登录页面

**实现**：
```javascript
const handleMenuClick = ({ key }) => {
  if (key === 'logout') {
    Modal.confirm({
      title: '确认退出',
      content: '确定要退出登录吗？',
      onOk() {
        logout() // 清除token、userId、username
        message.success('已退出登录')
        router.push('/auth')
      }
    })
  }
}
```

### 4. 路由守卫

**功能**：
- 未登录用户访问对话页面 → 自动跳转到登录页
- 已登录用户访问登录页 → 自动跳转到对话页
- 基于localStorage中的token判断登录状态

**实现**：
```javascript
router.beforeEach((to, from, next) => {
  const authenticated = isAuthenticated()

  if (to.meta.requiresAuth && !authenticated) {
    next('/auth')
  } else if (to.meta.requiresGuest && authenticated) {
    next('/')
  } else {
    next()
  }
})
```

## 样式设计

### 配色方案

**浅色主题**：
- 背景：`#ffffff` / `#f7f7f8`
- 文字：`#1a1a1a` / `#666666`
- 强调色：`#2563eb`（蓝色）
- 边框：`#e5e5e5`

**深色主题**：
- 背景：`#1a1a1a` / `#262626`
- 文字：`#f5f5f5` / `#a3a3a3`
- 强调色：`#3b82f6`（亮蓝色）
- 边框：`#404040`

### 设计原则

1. **简约至上**：去除所有渐变色和花哨效果
2. **一致性**：与对话界面使用相同的设计语言
3. **可访问性**：良好的对比度和可读性
4. **响应式**：适配各种屏幕尺寸

## 用户流程

### 首次访问
1. 访问根路径 `/`
2. 路由守卫检测未登录
3. 自动跳转到 `/auth`
4. 用户注册或登录
5. 成功后跳转到对话页面

### 已登录用户
1. 访问任意路径
2. 路由守卫检测已登录
3. 正常访问对话页面
4. 可以切换主题
5. 可以退出登录

### 退出登录
1. 点击右上角用户头像
2. 选择"退出登录"
3. 确认退出
4. 清除本地数据
5. 跳转到登录页

## 本地存储

应用使用localStorage存储以下信息：

```javascript
// 认证信息
localStorage.setItem('token', response.token)
localStorage.setItem('userId', response.userId)
localStorage.setItem('username', response.username)

// 主题设置
localStorage.setItem('springai-theme', 'light' | 'dark')

// 聊天记录
localStorage.setItem('springai-chats', JSON.stringify(chats))
localStorage.setItem('springai-active-chat', chatId)
```

## API集成

### 登录
```javascript
POST /api/auth/login
Body: { username, deviceId }
Response: { token, userId, username }
```

### 注册
```javascript
POST /api/auth/register
Body: { username, email, deviceId }
Response: { token, userId, username }
```

### 聊天
```javascript
POST /api/chat
Headers: { Authorization: Bearer {token} }
Body: { message, model, userId }
```

## 响应式设计

### 断点
- 桌面：> 768px
- 平板：481px - 768px
- 手机：≤ 480px

### 适配
- 登录卡片最大宽度：420px
- 移动端减小内边距
- Logo尺寸自适应
- 表单元素自适应

## 浏览器兼容性

- Chrome/Edge: ✅ 完全支持
- Firefox: ✅ 完全支持
- Safari: ✅ 完全支持
- 移动浏览器: ✅ 完全支持

## 注意事项

1. **主题切换**：主题状态在页面刷新后保持
2. **自动登录**：token有效期内自动保持登录状态
3. **安全性**：敏感操作（退出登录）需要二次确认
4. **用户体验**：所有操作都有即时反馈（message提示）

## 开发建议

1. **扩展功能**：
   - 添加"记住我"功能
   - 添加密码找回功能
   - 添加第三方登录

2. **优化**：
   - 添加骨架屏加载
   - 优化动画性能
   - 添加错误边界

3. **安全**：
   - 实现token刷新机制
   - 添加请求加密
   - 实现CSRF防护
