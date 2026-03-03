# Excel处理器项目

一个基于Vue.js和Spring Boot的Excel文件处理系统，支持Excel文件上传、可视化配置、数据导出和数据库存储。

## 项目结构

```
excel-processor/
├── backend/                # 后端Spring Boot项目
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/excel/
│   │   │   │   ├── controller/     # 控制器
│   │   │   │   ├── service/        # 服务层
│   │   │   │   ├── entity/         # 实体类
│   │   │   │   ├── repository/     # 数据访问层
│   │   │   │   └── dto/            # 数据传输对象
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   └── pom.xml
└── frontend/               # 前端Vue项目
    ├── public/
    ├── src/
    │   ├── components/     # Vue组件
    │   ├── api/           # API接口
    │   ├── App.vue
    │   └── main.js
    ├── package.json
    └── vue.config.js
```

## 功能特性

- **Excel文件上传**：支持.xlsx和.xls格式
- **数据可视化**：表格形式展示Excel数据
- **列选择配置**：可选择需要保留的列
- **数据导出**：导出处理后的Excel文件
- **数据库存储**：将选中的数据保存到数据库
- **数据管理**：查看和删除已保存的数据

## 技术栈

### 后端
- Spring Boot 2.7.18（兼容JDK 1.8）
- Apache POI 5.2.3（Excel处理）
- Spring Data JPA
- H2数据库（开发环境）
- MySQL（生产环境可选）

### 前端
- Vue.js 2.6.14
- Element UI 2.15.14
- Axios
- XLSX

## 快速开始

### 后端启动

1. 进入后端目录：
```bash
cd backend
```

2. 编译项目：
```bash
mvn clean install
```

3. 启动应用：
```bash
mvn spring-boot:run
```

后端服务将在 http://localhost:8081 启动

### 前端启动

1. 进入前端目录：
```bash
cd frontend
```

2. 安装依赖：
```bash
npm install
```

3. 启动开发服务器：
```bash
npm run serve
```

前端应用将在 http://localhost:8080 启动

## API接口

### 上传Excel文件
- **POST** `/api/excel/upload`
- 参数：`file` (MultipartFile)
- 返回：解析后的Excel数据

### 保存数据到数据库
- **POST** `/api/excel/save`
- 参数：JSON对象（fileName, sheetName, data, selectedColumns）
- 返回：操作结果

### 导出Excel
- **POST** `/api/excel/export-selected`
- 参数：Excel数据对象
- 返回：Excel文件流

### 获取已保存数据
- **GET** `/api/excel/selected-data`
- 返回：已保存的数据列表

### 删除数据
- **DELETE** `/api/excel/delete/{fileName}`
- 参数：文件名
- 返回：操作结果

## 数据库配置

### H2数据库（默认）
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
```

### MySQL数据库
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/excel_db?useSSL=false&serverTimezone=UTC
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=root
spring.datasource.password=your_password
```

## 使用说明

1. 打开浏览器访问 http://localhost:8080
2. 点击上传区域或拖拽Excel文件上传
3. 点击"上传并解析"按钮
4. 在数据预览区域查看解析结果
5. 勾选需要保留的列
6. 点击"保存到数据库"保存数据
7. 点击"导出Excel"下载处理后的文件

## 注意事项

- 支持的Excel文件格式：.xlsx、.xls
- 最大文件大小：50MB
- 数据预览默认显示前100行
- H2数据库控制台：http://localhost:8081/h2-console

## 构建生产版本

### 后端
```bash
cd backend
mvn clean package
```

### 前端
```bash
cd frontend
npm run build
```

## 许可证

MIT License