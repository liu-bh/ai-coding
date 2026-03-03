<template>
  <div id="app">
    <el-container>
      <el-header>
        <h1>Excel处理器</h1>
      </el-header>
      <el-main>
        <ExcelProcessor />
      </el-main>
    </el-container>
  </div>
</template>

<script>
import ExcelProcessor from './components/ExcelProcessor.vue'

export default {
  name: 'App',
  components: {
    ExcelProcessor
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
  text-align: center;
  line-height: 60px;
  padding: 0;
}

.el-header h1 {
  margin: 0;
  font-size: 24px;
}

.el-main {
  padding: 20px;
}
</style>