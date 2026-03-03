import axios from 'axios'

const API_BASE_URL = 'http://localhost:8081/api/excel'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export default {
  uploadExcel(formData, headerEndRow = 1) {
    formData.append('headerEndRow', headerEndRow)
    return axios.post(`${API_BASE_URL}/upload`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 60000
    })
  },
  
  saveToDatabase(data) {
    return apiClient.post('/save', data)
  },
  
  exportExcel(data) {
    return axios.post(`${API_BASE_URL}/export-selected`, data, {
      responseType: 'arraybuffer',
      timeout: 60000
    })
  },
  
  getSelectedData() {
    return apiClient.get('/selected-data')
  },
  
  deleteDataByFileName(fileName) {
    return apiClient.delete(`/delete/${encodeURIComponent(fileName)}`)
  }
}