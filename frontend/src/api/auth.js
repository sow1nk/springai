import axios from 'axios'

const API_BASE_URL = '/api'

// Generate UUID v4
const generateUUID = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0
    const v = c === 'x' ? r : (r & 0x3 | 0x8)
    return v.toString(16)
  })
}

// Get or create device UUID
const getDeviceUUID = () => {
  let deviceId = localStorage.getItem('deviceId')
  if (!deviceId) {
    deviceId = generateUUID()
    localStorage.setItem('deviceId', deviceId)
  }
  return deviceId
}

export const login = async (credentials) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/auth/login`, {
      ...credentials
    })
    return response.data
  } catch (error) {
    throw new Error(error.response?.data || '登录失败')
  }
}

export const register = async (userData) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/auth/register`, {
      ...userData
    })
    return response.data
  } catch (error) {
    throw new Error(error.response?.data || '注册失败')
  }
}

export const logout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
}

export const isAuthenticated = () => {
  return !!localStorage.getItem('token')
}

export const getToken = () => {
  return localStorage.getItem('token')
}

export const getUserInfo = () => {
  return {
    userId: localStorage.getItem('userId'),
    username: localStorage.getItem('username')
  }
}
