<template>
  <div class="excel-processor">
    <el-card class="upload-card">
      <div slot="header" class="clearfix">
        <span>上传Excel文件</span>
      </div>
      <el-upload
        class="upload-demo"
        drag
        action="#"
        :auto-upload="false"
        :on-change="handleFileChange"
        :file-list="fileList"
        accept=".xlsx,.xls"
      >
        <i class="el-icon-upload"></i>
        <div class="el-upload__text">将Excel文件拖到此处，或<em>点击上传</em></div>
        <div class="el-upload__tip" slot="tip">只能上传xlsx/xls文件</div>
      </el-upload>
      <el-button 
        type="primary" 
        @click="uploadFile" 
        :loading="uploading"
        :disabled="!selectedFile"
        style="margin-top: 20px;"
      >
        上传并解析
      </el-button>
    </el-card>

    <el-card v-if="excelData" class="data-card" style="margin-top: 20px;">
      <div slot="header" class="clearfix">
        <span>数据预览与配置</span>
        <el-button 
          style="float: right;" 
          type="primary" 
          size="small"
          @click="exportExcel"
        >
          导出Excel
        </el-button>
      </div>
      
      <el-tabs v-model="activeSheet" @tab-click="handleSheetChange">
        <el-tab-pane
          v-for="sheetName in excelData.sheetNames"
          :key="sheetName"
          :label="sheetName"
          :name="sheetName"
        >
          <div class="sheet-content">
            <div class="header-row-selector">
              <el-form label-width="120px" inline>
                <el-form-item label="表头结束行：">
                  <el-input-number
                    v-model="headerEndRows[sheetName]"
                    :min="1"
                    :max="excelData.sheetsData[sheetName]?.length || 10"
                    @change="handleHeaderRowChange(sheetName)"
                    size="small"
                  ></el-input-number>
                </el-form-item>
              </el-form>
            </div>
            
            <div class="column-manager">
              <div class="column-manager-header">
                <h4>列配置管理</h4>
                <el-button 
                  type="primary" 
                  size="small" 
                  icon="el-icon-plus"
                  @click="showAddColumnDialog(sheetName)"
                >
                  添加新列
                </el-button>
              </div>
              
              <el-table
                :data="getColumnConfigData(sheetName)"
                border
                style="width: 100%; margin-top: 10px;"
                max-height="300"
                row-key="originalName"
                @sort-change="handleColumnSort"
                @dragover.prevent
                @drop="handleTableDrop($event, sheetName)"
              >
                <el-table-column label="排序" width="120" align="center">
                  <template slot-scope="scope">
                    <div class="sort-buttons">
                      <el-button
                        type="info"
                        size="mini"
                        icon="el-icon-top"
                        circle
                        @click="moveColumnUp(sheetName, scope.$index)"
                        :disabled="scope.$index === 0"
                      ></el-button>
                      <el-button
                        type="info"
                        size="mini"
                        icon="el-icon-bottom"
                        circle
                        @click="moveColumnDown(sheetName, scope.$index)"
                        :disabled="scope.$index === getColumnConfigData(sheetName).length - 1"
                      ></el-button>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="选择" width="60" align="center">
                  <template slot-scope="scope">
                    <el-checkbox 
                      v-model="scope.row.selected"
                      @change="updateSelectedColumns(sheetName)"
                    ></el-checkbox>
                  </template>
                </el-table-column>
                <el-table-column prop="originalName" label="原始列名" min-width="150"></el-table-column>
                <el-table-column label="新列名" min-width="150">
                  <template slot-scope="scope">
                    <el-input 
                      v-model="scope.row.newName" 
                      placeholder="输入新列名"
                      size="small"
                      @change="applyColumnNameChange(sheetName, scope.$index, scope.row.newName)"
                    ></el-input>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="100" align="center">
                  <template slot-scope="scope">
                    <el-button 
                      type="danger" 
                      size="mini" 
                      icon="el-icon-delete"
                      circle
                      @click="deleteColumn(sheetName, scope.$index)"
                    ></el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            
            <el-table
              :data="getSheetData(sheetName)"
              border
              style="width: 100%; margin-top: 20px;"
              max-height="500"
            >
              <el-table-column
                v-for="(header, index) in getVisibleHeaders(sheetName)"
                :key="header"
                :prop="header"
                :label="header"
                min-width="150"
              >
                <template slot="header" slot-scope="{ column, $index }">
                  <div class="header-with-sort">
                    <span>{{ header }}</span>
                    <div class="header-sort-buttons">
                      <el-button
                        type="primary"
                        size="mini"
                        icon="el-icon-top"
                        circle
                        @click="moveDataColumnUp(sheetName, index)"
                        :disabled="index === 0"
                      ></el-button>
                      <el-button
                        type="primary"
                        size="mini"
                        icon="el-icon-bottom"
                        circle
                        @click="moveDataColumnDown(sheetName, index)"
                        :disabled="index === getVisibleHeaders(sheetName).length - 1"
                      ></el-button>
                    </div>
                  </div>
                </template>
                <template slot-scope="scope">
                  {{ scope.row[header] }}
                </template>
              </el-table-column>
            </el-table>
            
            <div class="action-buttons" style="margin-top: 20px;">
              <el-select 
                v-model="selectedDataSource[sheetName]" 
                placeholder="选择数据源"
                style="width: 200px; margin-right: 10px;"
              >
                <el-option
                  v-for="dataSource in dataSources"
                  :key="dataSource.id"
                  :label="dataSource.name"
                  :value="dataSource.id"
                />
              </el-select>
              <el-button 
                type="success" 
                @click="saveToDatabase(sheetName)"
                :loading="saving"
                :disabled="!selectedDataSource[sheetName]"
              >
                保存到数据库
              </el-button>
              <el-button 
                type="primary" 
                @click="exportCurrentSheet(sheetName)"
              >
                导出当前Sheet
              </el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-card v-if="savedData.length > 0" class="saved-data-card" style="margin-top: 20px;">
      <div slot="header" class="clearfix">
        <span>已保存的数据</span>
        <el-button 
          style="float: right;" 
          type="danger" 
          size="small"
          @click="clearSavedData"
        >
          清空数据
        </el-button>
      </div>
      <el-table
        :data="savedData"
        border
        style="width: 100%"
        max-height="300"
      >
        <el-table-column prop="fileName" label="文件名" width="200"></el-table-column>
        <el-table-column prop="sheetName" label="Sheet名" width="150"></el-table-column>
        <el-table-column prop="rowIndex" label="行号" width="80"></el-table-column>
        <el-table-column prop="columnName" label="列名" width="150"></el-table-column>
        <el-table-column prop="columnValue" label="值"></el-table-column>
      </el-table>
    </el-card>

    <el-dialog 
      title="添加新列" 
      :visible.sync="addColumnDialogVisible"
      width="500px"
    >
      <el-form :model="newColumnForm" label-width="100px">
        <el-form-item label="列名称">
          <el-input v-model="newColumnForm.columnName" placeholder="请输入列名称"></el-input>
        </el-form-item>
        <el-form-item label="默认值">
          <el-input v-model="newColumnForm.defaultValue" placeholder="请输入默认值（可选）"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="addColumnDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="addNewColumn">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import excelApi from '@/api/excel'
import datasourceApi from '@/api/datasource'

export default {
  name: 'ExcelProcessor',
  data() {
    return {
      selectedFile: null,
      fileList: [],
      uploading: false,
      saving: false,
      excelData: null,
      activeSheet: '',
      selectedColumns: {},
      columnConfigs: {},
      savedData: [],
      addColumnDialogVisible: false,
      newColumnForm: {
        sheetName: '',
        columnName: '',
        defaultValue: ''
      },
      headerEndRows: {},
      selectedDatabaseType: {},
      selectedDataSource: {},
      dataSources: [],
      databaseTypes: [
        { value: 'mysql', label: 'MySQL' },
        { value: 'postgresql', label: 'PostgreSQL' },
        { value: 'oracle', label: 'Oracle' },
        { value: 'sqlserver', label: 'SQL Server' },
        { value: 'h2', label: 'H2 (内存数据库)' }
      ],
      draggedIndex: -1
    }
  },
  mounted() {
    this.loadDataSources()
  },
  methods: {
    handleFileChange(file) {
      this.selectedFile = file.raw
      this.fileList = [file]
    },
    
    async loadDataSources() {
      try {
        const response = await datasourceApi.getDataSources()
        this.dataSources = response.data
      } catch (error) {
        console.error('加载数据源失败:', error)
      }
    },
    
    async uploadFile() {
      if (!this.selectedFile) {
        this.$message.warning('请选择要上传的文件')
        return
      }
      
      this.uploading = true
      try {
        const formData = new FormData()
        formData.append('file', this.selectedFile)
        
        const response = await excelApi.uploadExcel(formData, 1)
        
        if (response.data.message === '文件上传成功') {
          this.excelData = response.data.data
          this.activeSheet = this.excelData.sheetNames[0]
          
          this.excelData.sheetNames.forEach(sheetName => {
            this.$set(this.selectedColumns, sheetName, [...this.excelData.sheetHeaders[sheetName]])
            this.initColumnConfig(sheetName)
            this.$set(this.headerEndRows, sheetName, 1)
            this.$set(this.selectedDatabaseType, sheetName, 'h2') // 默认选择H2
          })
          
          this.$message.success('文件上传并解析成功')
        }
      } catch (error) {
        this.$message.error('文件上传失败: ' + (error.response?.data || error.message))
      } finally {
        this.uploading = false
      }
    },
    
    async handleHeaderRowChange(sheetName) {
      if (!this.selectedFile) {
        this.$message.warning('请重新上传文件')
        return
      }
      
      const headerEndRow = this.headerEndRows[sheetName]
      
      this.uploading = true
      try {
        const formData = new FormData()
        formData.append('file', this.selectedFile)
        
        const response = await excelApi.uploadExcel(formData, headerEndRow)
        
        if (response.data.message === '文件上传成功') {
          const newData = response.data.data
          
          // 只更新当前sheet的数据
          this.$set(this.excelData.sheetHeaders, sheetName, newData.sheetHeaders[sheetName])
          this.$set(this.excelData.sheetsData, sheetName, newData.sheetsData[sheetName])
          
          // 重新初始化列配置
          this.initColumnConfig(sheetName)
          
          // 更新选中列
          this.$set(this.selectedColumns, sheetName, [...newData.sheetHeaders[sheetName]])
          
          this.$message.success('表头行设置成功')
        }
      } catch (error) {
        this.$message.error('设置表头行失败: ' + (error.response?.data || error.message))
      } finally {
        this.uploading = false
      }
    },
    
    initColumnConfig(sheetName) {
      const headers = this.excelData.sheetHeaders[sheetName]
      const config = headers.map(header => ({
        originalName: header,
        newName: header,
        selected: true
      }))
      this.$set(this.columnConfigs, sheetName, config)
    },
    
    getColumnConfigData(sheetName) {
      return this.columnConfigs[sheetName] || []
    },
    
    updateSelectedColumns(sheetName) {
      const selected = this.columnConfigs[sheetName]
        .filter(col => col.selected)
        .map(col => col.newName)
      this.$set(this.selectedColumns, sheetName, selected)
    },
    
    applyColumnNameChange(sheetName, index, newName) {
      const oldName = this.columnConfigs[sheetName][index].originalName
      const headers = this.excelData.sheetHeaders[sheetName]
      const headerIndex = headers.indexOf(oldName)
      
      if (headerIndex !== -1) {
        this.$set(this.excelData.sheetHeaders[sheetName], headerIndex, newName)
        
        this.excelData.sheetsData[sheetName].forEach(row => {
          if (row.hasOwnProperty(oldName)) {
            row[newName] = row[oldName]
            delete row[oldName]
          }
        })
        
        const selectedIndex = this.selectedColumns[sheetName].indexOf(oldName)
        if (selectedIndex !== -1) {
          this.$set(this.selectedColumns[sheetName], selectedIndex, newName)
        }
      }
      
      this.$message.success('列名已更新')
    },
    
    deleteColumn(sheetName, index) {
      const columnName = this.columnConfigs[sheetName][index].originalName
      
      this.$confirm('确定要删除该列吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.columnConfigs[sheetName].splice(index, 1)
        
        const headerIndex = this.excelData.sheetHeaders[sheetName].indexOf(columnName)
        if (headerIndex !== -1) {
          this.excelData.sheetHeaders[sheetName].splice(headerIndex, 1)
        }
        
        this.excelData.sheetsData[sheetName].forEach(row => {
          delete row[columnName]
        })
        
        const selectedIndex = this.selectedColumns[sheetName].indexOf(columnName)
        if (selectedIndex !== -1) {
          this.selectedColumns[sheetName].splice(selectedIndex, 1)
        }
        
        this.$message.success('列已删除')
      }).catch(() => {})
    },
    
    showAddColumnDialog(sheetName) {
      this.newColumnForm = {
        sheetName: sheetName,
        columnName: '',
        defaultValue: ''
      }
      this.addColumnDialogVisible = true
    },
    
    addNewColumn() {
      if (!this.newColumnForm.columnName) {
        this.$message.warning('请输入列名称')
        return
      }
      
      const sheetName = this.newColumnForm.sheetName
      const columnName = this.newColumnForm.columnName
      const defaultValue = this.newColumnForm.defaultValue
      
      if (this.excelData.sheetHeaders[sheetName].includes(columnName)) {
        this.$message.warning('该列名已存在，请使用其他名称')
        return
      }
      
      this.excelData.sheetHeaders[sheetName].push(columnName)
      
      this.excelData.sheetsData[sheetName].forEach(row => {
        row[columnName] = defaultValue
      })
      
      this.columnConfigs[sheetName].push({
        originalName: columnName,
        newName: columnName,
        selected: true
      })
      
      this.selectedColumns[sheetName].push(columnName)
      
      this.addColumnDialogVisible = false
      this.$message.success('新列已添加')
    },
    
    handleSheetChange(tab) {
      this.activeSheet = tab.name
    },
    
    getSheetData(sheetName) {
      if (!this.excelData || !this.excelData.sheetsData[sheetName]) {
        return []
      }
      return this.excelData.sheetsData[sheetName].slice(0, 100)
    },
    
    getVisibleHeaders(sheetName) {
      return this.selectedColumns[sheetName] || []
    },
    
    async saveToDatabase(sheetName) {
      if (!this.selectedColumns[sheetName] || this.selectedColumns[sheetName].length === 0) {
        this.$message.warning('请至少选择一列')
        return
      }
      
      if (!this.selectedDataSource[sheetName]) {
        this.$message.warning('请选择数据源')
        return
      }
      
      this.saving = true
      try {
        const data = {
          dataSourceId: this.selectedDataSource[sheetName],
          fileName: this.excelData.fileName,
          sheetName: sheetName,
          data: this.excelData.sheetsData[sheetName],
          selectedColumns: this.selectedColumns[sheetName]
        }
        
        const response = await datasourceApi.saveDataToDataSource(data)
        if (response.data.success) {
          this.$message.success(`数据保存成功到数据源: ${response.data.dataSourceName}`)
        } else {
          this.$message.error('数据保存失败')
        }
        await this.loadSavedData()
      } catch (error) {
        this.$message.error('数据保存失败: ' + (error.response?.data || error.message))
      } finally {
        this.saving = false
      }
    },
    
    async exportExcel() {
      try {
        const exportData = this.prepareExportData()
        
        const response = await excelApi.exportExcel(exportData)
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', 'export.xlsx')
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (error) {
        this.$message.error('导出失败: ' + (error.response?.data || error.message))
      }
    },
    
    prepareExportData() {
      console.log('=== 前端导出调试 ===')
      console.log('excelData:', this.excelData)
      console.log('selectedColumns:', this.selectedColumns)
      console.log('columnConfigs:', this.columnConfigs)
      
      const exportData = {
        fileName: this.excelData.fileName,
        sheetNames: [],
        sheetHeaders: {},
        sheetsData: {}
      }
      
      this.excelData.sheetNames.forEach(sheetName => {
        const selectedHeaders = (this.selectedColumns[sheetName] || []).filter(h => h && h.trim() !== '')
        console.log(`Sheet ${sheetName} selectedHeaders:`, selectedHeaders)
        
        if (selectedHeaders.length === 0) return
        
        exportData.sheetNames.push(sheetName)
        exportData.sheetHeaders[sheetName] = selectedHeaders
        
        const filteredData = this.excelData.sheetsData[sheetName].map(row => {
          const filteredRow = {}
          selectedHeaders.forEach(header => {
            let value = row[header]
            console.log(`  Header: ${header}, Value:`, value, `Type: ${typeof value}`)
            if (value === null || value === undefined) {
              value = ''
            } else if (typeof value === 'object') {
              console.log(`  警告: 值是对象类型!`, value)
              value = JSON.stringify(value)
            }
            filteredRow[header] = value
          })
          return filteredRow
        })
        
        exportData.sheetsData[sheetName] = filteredData
      })
      
      console.log('最终导出数据:', JSON.stringify(exportData, null, 2))
      
      return exportData
    },
    
    async exportCurrentSheet(sheetName) {
      try {
        const selectedHeaders = (this.selectedColumns[sheetName] || []).filter(h => h && h.trim() !== '')
        if (selectedHeaders.length === 0) {
          this.$message.warning('请至少选择一列')
          return
        }
        
        const filteredData = this.excelData.sheetsData[sheetName].map(row => {
          const filteredRow = {}
          selectedHeaders.forEach(header => {
            let value = row[header]
            if (value === null || value === undefined) {
              value = ''
            } else if (typeof value === 'object') {
              value = JSON.stringify(value)
            }
            filteredRow[header] = value
          })
          return filteredRow
        })
        
        const exportData = {
          fileName: this.excelData.fileName,
          sheetNames: [sheetName],
          sheetHeaders: { [sheetName]: selectedHeaders },
          sheetsData: { [sheetName]: filteredData }
        }
        
        console.log('导出当前Sheet数据:', JSON.stringify(exportData, null, 2))
        
        const response = await excelApi.exportExcel(exportData)
        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', `${sheetName}.xlsx`)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (error) {
        this.$message.error('导出失败: ' + (error.response?.data || error.message))
      }
    },
    
    handleColumnSort({ column, prop, order }) {
      if (column && column.property === 'sortOrder') {
        const sheetName = this.activeSheet
        const configs = this.columnConfigs[sheetName]
        
        if (order === 'ascending') {
          // 这里我们使用一种简单的方式来调整顺序
          // 实际项目中可以使用更复杂的拖拽排序库
          this.$message.info('请使用拖拽方式调整列顺序')
        }
      }
    },
    
    handleDragStart(event, sheetName, index) {
      this.draggedIndex = index
      event.dataTransfer.effectAllowed = 'move'
      event.dataTransfer.setData('text/plain', index.toString())
      // 添加拖拽视觉效果
      event.target.style.opacity = '0.5'
    },
    
    handleDragEnd(event) {
      // 恢复按钮视觉效果
      event.target.style.opacity = '1'
    },
    
    handleDrop(event, sheetName, dropIndex) {
      event.preventDefault()
      if (this.draggedIndex !== -1 && this.draggedIndex !== dropIndex) {
        this.reorderColumns(sheetName, this.draggedIndex, dropIndex)
        this.draggedIndex = -1
      }
    },
    
    handleTableDrop(event, sheetName) {
      event.preventDefault()
      // 找到drop事件发生的行
      const target = event.target.closest('.el-table__row')
      if (target) {
        const rowIndex = target.rowIndex - 1 // 减去表头行
        if (this.draggedIndex !== -1 && this.draggedIndex !== rowIndex) {
          this.reorderColumns(sheetName, this.draggedIndex, rowIndex)
          this.draggedIndex = -1
        }
      }
    },
    
    moveColumnUp(sheetName, index) {
      if (index > 0) {
        this.reorderColumns(sheetName, index, index - 1)
      }
    },
    
    moveColumnDown(sheetName, index) {
      const configs = this.columnConfigs[sheetName]
      if (index < configs.length - 1) {
        this.reorderColumns(sheetName, index, index + 1)
      }
    },
    
    moveDataColumnUp(sheetName, index) {
      if (index > 0) {
        this.reorderSelectedColumns(sheetName, index, index - 1)
      }
    },
    
    moveDataColumnDown(sheetName, index) {
      const selectedCols = this.selectedColumns[sheetName]
      if (index < selectedCols.length - 1) {
        this.reorderSelectedColumns(sheetName, index, index + 1)
      }
    },
    
    reorderSelectedColumns(sheetName, fromIndex, toIndex) {
      const selectedCols = this.selectedColumns[sheetName]
      const configs = this.columnConfigs[sheetName]
      
      // 重新排序选中列
      const movedCol = selectedCols.splice(fromIndex, 1)[0]
      selectedCols.splice(toIndex, 0, movedCol)
      
      // 重新排序配置，保持与选中列一致
      const newConfigs = []
      selectedCols.forEach(colName => {
        const config = configs.find(c => c.newName === colName)
        if (config) {
          newConfigs.push(config)
        }
      })
      
      // 添加未选中的列到末尾
      configs.forEach(config => {
        if (!newConfigs.find(c => c.newName === config.newName)) {
          newConfigs.push(config)
        }
      })
      
      // 更新配置和表头
      this.$set(this.columnConfigs, sheetName, newConfigs)
      this.excelData.sheetHeaders[sheetName] = newConfigs.map(config => config.newName)
      
      this.$message.success('列顺序已调整')
    },
    
    reorderColumns(sheetName, fromIndex, toIndex) {
      const configs = this.columnConfigs[sheetName]
      
      // 重新排序配置
      const movedConfig = configs.splice(fromIndex, 1)[0]
      configs.splice(toIndex, 0, movedConfig)
      
      // 更新表头
      this.excelData.sheetHeaders[sheetName] = configs.map(config => config.newName)
      
      // 更新选中列
      const selectedCols = this.selectedColumns[sheetName]
      const newSelectedCols = []
      configs.forEach(config => {
        if (config.selected && selectedCols.includes(config.newName)) {
          newSelectedCols.push(config.newName)
        }
      })
      this.$set(this.selectedColumns, sheetName, newSelectedCols)
      
      this.$message.success('列顺序已调整')
    },
    
    async loadSavedData() {
      try {
        const response = await excelApi.getSelectedData()
        this.savedData = response.data.data || []
      } catch (error) {
        console.error('加载已保存数据失败:', error)
      }
    },
    
    async clearSavedData() {
      try {
        await this.$confirm('确定要清空所有已保存的数据吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        const fileNames = [...new Set(this.savedData.map(item => item.fileName))]
        for (const fileName of fileNames) {
          await excelApi.deleteDataByFileName(fileName)
        }
        
        this.savedData = []
        this.$message.success('数据已清空')
      } catch (error) {
        if (error !== 'cancel') {
          this.$message.error('清空数据失败: ' + (error.response?.data || error.message))
        }
      }
    }
  },
  mounted() {
    this.loadSavedData()
  }
}
</script>

<style scoped>
.excel-processor {
  max-width: 1400px;
  margin: 0 auto;
}

.upload-card, .data-card, .saved-data-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.column-manager {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-bottom: 20px;
}

.sort-buttons {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.header-with-sort {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.header-sort-buttons {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.header-sort-buttons .el-button {
  padding: 2px;
  min-width: 20px;
  height: 20px;
}

.column-manager-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.column-manager-header h4 {
  margin: 0;
  color: #606266;
}

.action-buttons {
  text-align: center;
}

.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}

.clearfix:after {
  clear: both;
}
</style>