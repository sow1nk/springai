import axios from 'axios'
import { getToken } from './auth'

const API_BASE_URL = '/api'

// Add JWT token to all requests
axios.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Handle 401 responses
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // Redirect to login
      localStorage.removeItem('token')
      localStorage.removeItem('userId')
      localStorage.removeItem('username')
      window.location.href = '/auth'
    }
    return Promise.reject(error)
  }
)

export const sendMessage = async (message, model, conversationId) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/chat`, {
      message,
      model,
      conversationId
    })
    return response.data
  } catch (error) {
    throw new Error(error.response?.data || '发送消息失败')
  }
}

export const getChatHistory = async (userId) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/chat/history`, {
      params: { userId }
    })
    return response.data
  } catch (error) {
    throw new Error(error.response?.data || '获取聊天记录失败')
  }
}

// TODO: 当前前端没有按 sessionId 加载历史记录的需求，如无必要可移除该接口
export const getChatHistoryBySession = async (userId, sessionId) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/chat/history/${sessionId}`, {
      params: { userId }
    })
    return response.data
  } catch (error) {
    throw new Error(error.response?.data || '获取会话记录失败')
  }
}

// TODO: 会话列表展示暂未调用该接口，待后续引导页或列表功能再接入
export const getUserConversations = async () => {
  try {
    const response = await axios.get(`${API_BASE_URL}/chat/conversations`)
    return response.data
  } catch (error) {
    throw new Error(error.response?.data || '获取会话列表失败')
  }
}

// TODO: 会话详情页尚未实现，暂时没有地方调用该接口
export const getConversationMessages = async (conversationId) => {
  try {
    const response = await axios.get(`${API_BASE_URL}/chat/conversations/${conversationId}/messages`)
    return response.data
  } catch (error) {
    throw new Error(error.response?.data || '获取会话消息失败')
  }
}

export const deleteConversation = async (conversationId) => {
  try {
    const response = await axios.delete(`${API_BASE_URL}/chat/conversations/${conversationId}`)
    return response.data
  } catch (error) {
    throw new Error(error.response?.data || '删除会话失败')
  }
}
