import axios from 'axios'
import { message as antMessage } from 'ant-design-vue'

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
export const sendChatMessage = async (message, model) => {
  try {
    const response = await axios.post('/chat', {
      message,
      model
    }, {
      headers: {
        'Content-Type': 'application/json'
      }
    })
    return response.data
  } catch (error) {
    console.error('Send message failed:', error)
    throw error
  }
}

/**
 * Send chat message with streaming response
 * @param {string} message - User message content
 * @param {string} model - Model name (deepseek or qwen)
 * @param {Function} onChunk - Callback when receiving data chunk
 * @param {AbortSignal} signal - Signal for canceling request
 * @returns {Promise<string>} Complete AI response content
 */
export const sendChatMessageStream = async (message, model, onChunk, signal) => {
  try {
    const response = await fetch('/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ message, model }),
      signal
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let fullMessage = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const chunk = decoder.decode(value, { stream: true })
      fullMessage += chunk

      if (onChunk) {
        onChunk(chunk, fullMessage)
      }
    }

    return fullMessage
  } catch (error) {
    if (error.name === 'AbortError') {
      console.log('Request cancelled')
      throw error
    }

    console.error('Send message failed:', error)

    let errorMessage = 'Send message failed'
    if (error.message.includes('Failed to fetch')) {
      errorMessage = 'Cannot connect to server, please check network and backend service'
    } else if (error.message.includes('HTTP error')) {
      errorMessage = `Server error: ${error.message}`
    }

    antMessage.error(errorMessage)
    throw error
  }
}

export default request
