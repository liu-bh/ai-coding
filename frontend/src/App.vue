<template>
  <div id="app">
    <el-container>
      <el-header>
        <div class="header-content">
          <h1>Excel处理器</h1>
          <el-menu :default-active="activeMenu" class="el-menu-demo" mode="horizontal" @select="handleMenuSelect">
            <el-menu-item index="excel">Excel处理</el-menu-item>
            <el-menu-item index="datasource">数据源管理</el-menu-item>
          </el-menu>
        </div>
      </el-header>
      <el-main>
        <ExcelProcessor v-if="activeMenu === 'excel'" />
        <DataSourceManager v-if="activeMenu === 'datasource'" />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import ExcelProcessor from './components/ExcelProcessor.vue'
import DataSourceManager from './components/DataSourceManager.vue'

export default {
  name: 'App',
  components: {
    ExcelProcessor,
    DataSourceManager
  },
  data() {
    return {
      activeMenu: 'excel'
    }
  },
  methods: {
    handleMenuSelect(key) {
      this.activeMenu = key
    }
  }
}
</script>

<style>
#app {
  font-family: 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
}

.el-header {
  background-color: #409EFF;
  color: white;
  padding: 0 20px;
  height: 60px;
  line-height: 60px;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.el-header h1 {
  margin: 0;
  font-size: 24px;
}

.el-main {
  padding: 20px;
}

.el-menu-demo {
  background-color: transparent;
  border: none;
}

.el-menu-demo .el-menu-item {
  color: white;
}

.el-menu-demo .el-menu-item.is-active {
  background-color: rgba(255, 255, 255, 0.2);
  color: white;
}
</style>