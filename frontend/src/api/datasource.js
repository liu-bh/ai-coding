import axios from 'axios'

const API_BASE_URL = 'http://localhost:8081/api/datasource'

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

export default {
  // 获取所有数据源
  getDataSources() {
    return apiClient.get('/list')
  },
  
  // 根据ID获取数据源
  getDataSourceById(id) {
    return apiClient.get(`/${id}`)
  },
  
  // 创建数据源
  createDataSource(dataSource) {
    return apiClient.post('/create', dataSource)
  },
  
  // 更新数据源
  updateDataSource(id, dataSource) {
    return apiClient.put(`/${id}`, dataSource)
  },
  
  // 删除数据源
  deleteDataSource(id) {
    return apiClient.delete(`/${id}`)
  },
  
  // 测试数据源连接
  testConnection(dataSource) {
    return apiClient.post('/test', dataSource)
  },
  
  // 保存数据到指定数据源
  saveDataToDataSource(data) {
    return apiClient.post('/save-data', data)
  }
}