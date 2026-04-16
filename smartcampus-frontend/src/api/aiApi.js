import api from './axiosInstance'

export const aiApi = {
  chat: (message) => api.post('/api/v1/ai/chat', { message }),
}
