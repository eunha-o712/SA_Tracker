import axios from 'axios'
import { readAuthSession } from '../utils/authSession'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8085',
})

api.interceptors.request.use((config) => {
  const token = readAuthSession()?.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export function getApiErrorMessage(error, fallbackMessage) {
  const responseMessage = error?.response?.data?.message

  if (typeof responseMessage === 'string' && responseMessage.trim()) {
    return responseMessage.trim()
  }

  if (!error?.response) {
    return '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.'
  }

  return fallbackMessage
}

export default api
