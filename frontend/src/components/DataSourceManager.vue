<template>
  <div class="datasource-manager">
    <el-card>
      <div slot="header" class="clearfix">
        <span>数据源管理</span>
        <el-button 
          style="float: right; padding: 3px 0" 
          type="text" 
          @click="showCreateDialog = true"
        >
          添加数据源
        </el-button>
      </div>
      
      <el-table :data="dataSources" style="width: 100%">
        <el-table-column prop="name" label="数据源名称" width="150" />
        <el-table-column prop="type" label="类型" width="120">
          <template slot-scope="scope">
            <el-tag :type="getTypeTagType(scope.row.type)">
              {{ getTypeLabel(scope.row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="host" label="主机" width="120" />
        <el-table-column prop="port" label="端口" width="80" />
        <el-table-column prop="databaseName" label="数据库名" width="120" />
        <el-table-column prop="username" label="用户名" width="100" />
        <el-table-column label="状态" width="80">
          <template slot-scope="scope">
            <el-tag :type="scope.row.isActive ? 'success' : 'danger'">
              {{ scope.row.isActive ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template slot-scope="scope">
            <el-button size="mini" @click="testConnection(scope.row)">测试</el-button>
            <el-button size="mini" type="primary" @click="editDataSource(scope.row)">编辑</el-button>
            <el-button size="mini" type="danger" @click="deleteDataSource(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <!-- 创建/编辑数据源对话框 -->
    <el-dialog 
      :title="isEdit ? '编辑数据源' : '添加数据源'" 
      :visible.sync="showCreateDialog"
      width="600px"
    >
      <el-form :model="formData" :rules="rules" ref="dataSourceForm" label-width="100px">
        <el-form-item label="数据源名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入数据源名称" />
        </el-form-item>
        
        <el-form-item label="数据库类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择数据库类型" @change="onTypeChange">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="Oracle" value="oracle" />
            <el-option label="SQL Server" value="sqlserver" />
            <el-option label="MongoDB" value="mongodb" />
            <el-option label="Redis" value="redis" />
          </el-select>
        </el-form-item>
        
        <el-form-item label="主机地址" prop="host">
          <el-input v-model="formData.host" placeholder="请输入主机地址" @change="updateConnectionString" />
        </el-form-item>
        
        <el-form-item label="端口" prop="port">
          <el-input-number v-model="formData.port" :min="1" :max="65535" @change="updateConnectionString" />
        </el-form-item>
        
        <el-form-item label="数据库名" prop="databaseName" v-if="showDatabaseName">
          <el-input v-model="formData.databaseName" placeholder="请输入数据库名" @change="updateConnectionString" />
        </el-form-item>
        
        <el-form-item label="用户名" prop="username" v-if="showUsername">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        
        <el-form-item label="密码" prop="password" v-if="showPassword">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        
        <el-form-item label="JDBC链接" prop="connectionString">
          <el-input 
            v-model="formData.connectionString" 
            type="textarea" 
            placeholder="JDBC连接字符串" 
            rows="3"
            @change="parseConnectionString"
          />
          <div style="margin-top: 10px; font-size: 12px; color: #606266;">
            <span>自动生成的JDBC链接：</span>
            <span style="font-family: monospace; background: #f5f7fa; padding: 2px 4px; border-radius: 2px;">{{ generatedConnectionString }}</span>
            <el-button 
              type="text" 
              size="small" 
              @click="useGeneratedConnectionString"
              style="margin-left: 10px;"
              :disabled="!generatedConnectionString"
            >
              使用此链接
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      
      <div slot="footer" class="dialog-footer">
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="info" @click="testConnection(formData)" :loading="testing">测试连接</el-button>
        <el-button type="primary" @click="saveDataSource" :loading="saving">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import datasourceApi from '@/api/datasource'

export default {
  name: 'DataSourceManager',
  data() {
    return {
      dataSources: [],
      showCreateDialog: false,
      isEdit: false,
      saving: false,
      formData: {
        name: '',
        type: '',
        host: '',
        port: 3306,
        databaseName: '',
        username: '',
        password: '',
        connectionString: ''
      },
      rules: {
        name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
        type: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
        host: [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
        port: [{ required: true, message: '请输入端口', trigger: 'blur' }]
      },
      showDatabaseName: true,
      showUsername: true,
      showPassword: true,
      testing: false
    }
  },
  computed: {
    generatedConnectionString() {
      const { type, host, port, databaseName } = this.formData
      if (!type || !host || !port) {
        return ''
      }
      
      switch (type) {
        case 'mysql':
          return `jdbc:mysql://${host}:${port}/${databaseName || 'test'}?useSSL=false&serverTimezone=UTC`
        case 'postgresql':
          return `jdbc:postgresql://${host}:${port}/${databaseName || 'test'}`
        case 'oracle':
          return `jdbc:oracle:thin:@${host}:${port}:${databaseName || 'ORCL'}`
        case 'sqlserver':
          return `jdbc:sqlserver://${host}:${port};databaseName=${databaseName || 'test'}`
        case 'h2':
          return `jdbc:h2:mem:${databaseName || 'test'}`
        case 'mongodb':
          return `mongodb://${host}:${port}/${databaseName || 'test'}`
        case 'redis':
          return `redis://${host}:${port}`
        default:
          return ''
      }
    }
  },
  mounted() {
    this.loadDataSources()
  },
  methods: {
    async loadDataSources() {
      try {
        const response = await datasourceApi.getDataSources()
        this.dataSources = response.data
      } catch (error) {
        this.$message.error('加载数据源失败: ' + (error.response?.data || error.message))
      }
    },
    
    onTypeChange(type) {
      // 根据数据库类型设置默认端口和显示字段
      const typeConfigs = {
        mysql: { port: 3306, showDatabaseName: true, showUsername: true, showPassword: true },
        postgresql: { port: 5432, showDatabaseName: true, showUsername: true, showPassword: true },
        oracle: { port: 1521, showDatabaseName: true, showUsername: true, showPassword: true },
        sqlserver: { port: 1433, showDatabaseName: true, showUsername: true, showPassword: true },
        mongodb: { port: 27017, showDatabaseName: true, showUsername: true, showPassword: false },
        redis: { port: 6379, showDatabaseName: false, showUsername: true, showPassword: true }
      }
      
      const config = typeConfigs[type] || typeConfigs.mysql
      this.formData.port = config.port
      this.showDatabaseName = config.showDatabaseName
      this.showUsername = config.showUsername
      this.showPassword = config.showPassword
      
      // 更新JDBC连接字符串
      this.updateConnectionString()
    },
    
    getTypeLabel(type) {
      const labels = {
        mysql: 'MySQL',
        postgresql: 'PostgreSQL',
        oracle: 'Oracle',
        sqlserver: 'SQL Server',
        mongodb: 'MongoDB',
        redis: 'Redis'
      }
      return labels[type] || type
    },
    
    getTypeTagType(type) {
      const types = {
        mysql: 'primary',
        postgresql: 'success',
        oracle: 'warning',
        sqlserver: 'info',
        mongodb: 'danger',
        redis: 'primary'
      }
      return types[type] || 'info'
    },
    
    async testConnection(dataSource) {
      this.testing = true
      try {
        const response = await datasourceApi.testConnection(dataSource)
        if (response.data.success) {
          this.$message.success('连接测试成功')
        } else {
          this.$message.error('连接测试失败')
        }
      } catch (error) {
        this.$message.error('连接测试失败: ' + (error.response?.data || error.message))
      } finally {
        this.testing = false
      }
    },
    
    useGeneratedConnectionString() {
      this.formData.connectionString = this.generatedConnectionString
      this.$message.success('已使用自动生成的JDBC链接')
    },
    
    parseConnectionString() {
      const connectionString = this.formData.connectionString
      if (!connectionString) return
      
      // 解析MySQL连接字符串
      const mysqlMatch = connectionString.match(/jdbc:mysql:\/\/(.*?):(\d+)\/(.*?)[?;]/)
      if (mysqlMatch) {
        this.formData.type = 'mysql'
        this.formData.host = mysqlMatch[1]
        this.formData.port = parseInt(mysqlMatch[2])
        this.formData.databaseName = mysqlMatch[3]
        this.onTypeChange('mysql')
        this.$message.success('已解析MySQL连接字符串')
        return
      }
      
      // 解析PostgreSQL连接字符串
      const postgresMatch = connectionString.match(/jdbc:postgresql:\/\/(.*?):(\d+)\/(.*)/)
      if (postgresMatch) {
        this.formData.type = 'postgresql'
        this.formData.host = postgresMatch[1]
        this.formData.port = parseInt(postgresMatch[2])
        this.formData.databaseName = postgresMatch[3]
        this.onTypeChange('postgresql')
        this.$message.success('已解析PostgreSQL连接字符串')
        return
      }
      
      // 解析Oracle连接字符串
      const oracleMatch = connectionString.match(/jdbc:oracle:thin:@(.*?):(\d+):(.*)/)
      if (oracleMatch) {
        this.formData.type = 'oracle'
        this.formData.host = oracleMatch[1]
        this.formData.port = parseInt(oracleMatch[2])
        this.formData.databaseName = oracleMatch[3]
        this.onTypeChange('oracle')
        this.$message.success('已解析Oracle连接字符串')
        return
      }
      
      // 解析SQL Server连接字符串
      const sqlserverMatch = connectionString.match(/jdbc:sqlserver:\/\/(.*?):(\d+);databaseName=(.*)/)
      if (sqlserverMatch) {
        this.formData.type = 'sqlserver'
        this.formData.host = sqlserverMatch[1]
        this.formData.port = parseInt(sqlserverMatch[2])
        this.formData.databaseName = sqlserverMatch[3]
        this.onTypeChange('sqlserver')
        this.$message.success('已解析SQL Server连接字符串')
        return
      }
      
      // 解析H2连接字符串
      const h2Match = connectionString.match(/jdbc:h2:mem:(.*)/)
      if (h2Match) {
        this.formData.type = 'h2'
        this.formData.host = 'localhost'
        this.formData.port = 9092
        this.formData.databaseName = h2Match[1]
        this.onTypeChange('h2')
        this.$message.success('已解析H2连接字符串')
        return
      }
      
      // 解析MongoDB连接字符串
      const mongodbMatch = connectionString.match(/mongodb:\/\/(.*?):(\d+)\/(.*)/)
      if (mongodbMatch) {
        this.formData.type = 'mongodb'
        this.formData.host = mongodbMatch[1]
        this.formData.port = parseInt(mongodbMatch[2])
        this.formData.databaseName = mongodbMatch[3]
        this.onTypeChange('mongodb')
        this.$message.success('已解析MongoDB连接字符串')
        return
      }
      
      // 解析Redis连接字符串
      const redisMatch = connectionString.match(/redis:\/\/(.*?):(\d+)/)
      if (redisMatch) {
        this.formData.type = 'redis'
        this.formData.host = redisMatch[1]
        this.formData.port = parseInt(redisMatch[2])
        this.formData.databaseName = ''
        this.onTypeChange('redis')
        this.$message.success('已解析Redis连接字符串')
        return
      }
    },
    
    updateConnectionString() {
      // 当主机、端口或数据库名改变时，自动更新JDBC连接字符串
      if (!this.formData.connectionString || this.formData.connectionString === this.generatedConnectionString) {
        this.formData.connectionString = this.generatedConnectionString
      }
    },
    
    editDataSource(dataSource) {
      this.isEdit = true
      this.formData = { ...dataSource }
      this.onTypeChange(dataSource.type)
      this.showCreateDialog = true
    },
    
    async deleteDataSource(dataSource) {
      try {
        await this.$confirm('确定要删除这个数据源吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await datasourceApi.deleteDataSource(dataSource.id)
        this.$message.success('删除成功')
        this.loadDataSources()
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('删除失败: ' + (error.response?.data || error.message))
        }
      }
    },
    
    async saveDataSource() {
      this.$refs.dataSourceForm.validate(async (valid) => {
        if (valid) {
          this.saving = true
          try {
            if (this.isEdit) {
              await datasourceApi.updateDataSource(this.formData.id, this.formData)
              this.$message.success('更新成功')
            } else {
              await datasourceApi.createDataSource(this.formData)
              this.$message.success('创建成功')
            }
            
            this.showCreateDialog = false
            this.loadDataSources()
            this.resetForm()
          } catch (error) {
            this.$message.error('保存失败: ' + (error.response?.data || error.message))
          } finally {
            this.saving = false
          }
        }
      })
    },
    
    resetForm() {
      this.formData = {
        name: '',
        type: '',
        host: '',
        port: 3306,
        databaseName: '',
        username: '',
        password: '',
        connectionString: ''
      }
      this.isEdit = false
    }
  }
}
</script>

<style scoped>
.datasource-manager {
  padding: 20px;
}
</style>