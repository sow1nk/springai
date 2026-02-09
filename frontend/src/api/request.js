import axios from 'axios'
import { message as antMessage } from 'ant-design-vue'
import { getToken, getUserInfo } from './auth'
import { fetchEventSource } from '@microsoft/fetch-event-source'

// Create axios instance
const request = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Request interceptor
request.interceptors.request.use(
  config => {
    // Add JWT token
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }

    // Add userId to request body for POST/PUT requests
    const userInfo = getUserInfo()
    if (userInfo.userId && (config.method === 'post' || config.method === 'put')) {
      if (config.data) {
        config.data = {
          ...config.data,
          userId: userInfo.userId
        }
      }
    }

    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor
request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    console.error('Response error:', error)

    let errorMessage = 'Request failed'

    if (error.response) {
      // Server returned error status code
      const status = error.response.status
      switch (status) {
        case 400:
          errorMessage = 'Bad request parameters'
          break
        case 401:
          errorMessage = 'Unauthorized, please login again'
          break
        case 403:
          errorMessage = 'Access denied'
          break
        case 404:
          errorMessage = 'Resource not found'
          break
        case 500:
          errorMessage = 'Internal server error'
          break
        case 502:
          errorMessage = 'Bad gateway'
          break
        case 503:
          errorMessage = 'Service unavailable'
          break
        default:
          errorMessage = `Request failed (${status})`
      }
    } else if (error.request) {
      // Request sent but no response received
      errorMessage = 'Cannot connect to server, please check network'
    } else {
      // Other errors
      errorMessage = error.message || 'Request failed'
    }

    antMessage.error(errorMessage)
    return Promise.reject(error)
  }
)

/**
 * Send chat message (non-streaming)
 * @param {string} message - User message content
 * @param {string} model - Model name (deepseek or qwen)
 * @returns {Promise<string>} AI response content
 */
// TODO: 目前前端只使用流式接口，该非流式方法暂未被引用，确认需求后再决定去留
export const sendChatMessage = async (message, model) => {
  try {
    const userInfo = getUserInfo()
    const token = getToken()

    const headers = {
      'Content-Type': 'application/json'
    }

    if (token) {
      headers.Authorization = `Bearer ${token}`
    }

    const response = await axios.post('/chat', {
      message,
      model,
      userId: userInfo.userId
    }, {
      headers
    })
    return response.data
  } catch (error) {
    console.error('Send message failed:', error)
    throw error
  }
}

/**
 * Send chat message with streaming SSE response
 * @param {string} message - User message content
 * @param {string} model - Model name (deepseek or qwen)
 * @param {Function} onEvent - Callback: (eventType: string, data: object) => void
 * @param {AbortSignal} signal - Signal for canceling request
 * @param {string} sessionId - Session ID for grouping messages
 * @returns {Promise<string>} Complete AI response content
 */
export const sendChatMessageStream = async (message, model, onEvent, signal, sessionId) => {
  const userInfo = getUserInfo()
  const token = getToken()

  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  const requestBody = { message, model, userId: userInfo.userId }
  if (sessionId) requestBody.sessionId = sessionId

  let fullResponse = ''

  await fetchEventSource('/chat', {
    method: 'POST',
    headers,
    body: JSON.stringify(requestBody),
    signal,
    onmessage(event) {
      const parsed = JSON.parse(event.data)

      if (event.event === 'step') {
        onEvent('step', parsed)
      } else if (event.event === 'token') {
        fullResponse += parsed.content
        onEvent('token', { content: fullResponse })
      } else if (event.event === 'done') {
        onEvent('done', parsed)
      } else if (event.event === 'error') {
        onEvent('error', parsed)
      }
    },
    onerror(err) {
      throw err
    },
    openWhenHidden: true,
  })

  return fullResponse
}

// TODO: request 实例暂未在组件中直接引入，若无统一封装需求可移除
export default request
