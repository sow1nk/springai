import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import './styles/theme.css'
import App from './App.vue'
import router from './router'

// 初始化主题
const savedTheme = localStorage.getItem('springai-theme') || 'light'
document.documentElement.setAttribute('data-theme', savedTheme)

const app = createApp(App)

app.use(Antd)
app.use(router)
app.mount('#app')
