import axios from 'axios'
import { v4 as uuidv4 } from 'uuid'

const apiClient = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  config.headers['X-Correlation-ID'] = uuidv4()
  return config
})

export default apiClient
