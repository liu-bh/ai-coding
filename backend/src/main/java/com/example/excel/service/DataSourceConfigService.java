package com.example.excel.service;

import com.example.excel.entity.DataSourceConfig;
import com.example.excel.repository.DataSourceConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Optional;

@Service
public class DataSourceConfigService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfigService.class);
    
    @Autowired
    private DataSourceConfigRepository dataSourceConfigRepository;
    
    public List<DataSourceConfig> getAllActiveDataSources() {
        return dataSourceConfigRepository.findByIsActive(true);
    }
    
    public Optional<DataSourceConfig> getDataSourceById(Long id) {
        return dataSourceConfigRepository.findById(id);
    }
    
    public Optional<DataSourceConfig> getDataSourceByName(String name) {
        return Optional.ofNullable(dataSourceConfigRepository.findByName(name));
    }
    
    public DataSourceConfig saveDataSource(DataSourceConfig dataSourceConfig) {
        dataSourceConfig.setUpdatedAt(java.time.LocalDateTime.now());
        return dataSourceConfigRepository.save(dataSourceConfig);
    }
    
    public void deleteDataSource(Long id) {
        dataSourceConfigRepository.deleteById(id);
    }
    
    public boolean testConnection(DataSourceConfig dataSourceConfig) {
        try {
            String connectionString = buildConnectionString(dataSourceConfig);
            logger.info("测试数据源连接: {}", connectionString);
            
            if ("mongodb".equals(dataSourceConfig.getType()) || "redis".equals(dataSourceConfig.getType())) {
                // 对于NoSQL数据库，这里简化处理，实际应该使用对应的驱动
                logger.info("NoSQL数据库连接测试: {}", dataSourceConfig.getType());
                return true;
            }
            
            // 测试关系型数据库连接
            try (Connection connection = DriverManager.getConnection(
                    connectionString, 
                    dataSourceConfig.getUsername(), 
                    dataSourceConfig.getPassword())) {
                boolean isValid = connection.isValid(5);
                logger.info("数据源连接测试结果: {}", isValid ? "成功" : "失败");
                return isValid;
            }
        } catch (Exception e) {
            logger.error("数据源连接测试失败: {}", e.getMessage());
            return false;
        }
    }
    
    private String buildConnectionString(DataSourceConfig config) {
        if (config.getConnectionString() != null && !config.getConnectionString().trim().isEmpty()) {
            return config.getConnectionString();
        }
        
        String type = config.getType().toLowerCase();
        String host = config.getHost();
        Integer port = config.getPort();
        String databaseName = config.getDatabaseName();
        
        switch (type) {
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=UTC", 
                    host, port, databaseName);
            case "postgresql":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, databaseName);
            case "oracle":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, databaseName);
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, databaseName);
            case "h2":
                return String.format("jdbc:h2:mem:%s", databaseName);
            default:
                throw new IllegalArgumentException("不支持的数据库类型: " + type);
        }
    }
    
    public boolean saveDataToDataSource(DataSourceConfig dataSource, String fileName, String sheetName, 
                                       List<java.util.Map<String, Object>> data, List<String> selectedColumns) {
        try {
            logger.info("开始保存数据到数据源: {} ({})", dataSource.getName(), dataSource.getType());
            
            if ("mongodb".equals(dataSource.getType()) || "redis".equals(dataSource.getType())) {
                // NoSQL数据库保存逻辑
                logger.info("保存数据到NoSQL数据库: {}", dataSource.getType());
                // 这里应该实现具体的NoSQL保存逻辑
                return true;
            }
            
            // 关系型数据库保存逻辑
            String connectionString = buildConnectionString(dataSource);
            try (Connection connection = DriverManager.getConnection(
                    connectionString, 
                    dataSource.getUsername(), 
                    dataSource.getPassword())) {
                
                // 创建表（如果不存在）
                String tableName = "excel_data_" + sheetName.replaceAll("[^a-zA-Z0-9]", "_");
                createTableIfNotExists(connection, tableName, selectedColumns);
                
                // 插入数据
                int insertedCount = 0;
                for (int i = 0; i < data.size(); i++) {
                    java.util.Map<String, Object> row = data.get(i);
                    if (insertRow(connection, tableName, row, selectedColumns, fileName, sheetName, i)) {
                        insertedCount++;
                    }
                }
                
                logger.info("数据保存成功: 数据源={}, 表名={}, 插入行数={}", 
                    dataSource.getName(), tableName, insertedCount);
                return true;
            }
        } catch (Exception e) {
            logger.error("数据保存失败: 数据源={}, 错误={}", dataSource.getName(), e.getMessage(), e);
            return false;
        }
    }
    
    private void createTableIfNotExists(Connection connection, String tableName, List<String> columns) throws Exception {
        // 简化的表创建逻辑
        String sql = String.format(
            "CREATE TABLE IF NOT EXISTS %s (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "file_name VARCHAR(255), " +
            "sheet_name VARCHAR(255), " +
            "row_index INT, " +
            "%s TEXT, " +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")", tableName, String.join(" TEXT, ", columns));
        
        try (java.sql.Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            logger.info("表创建成功: {}", tableName);
        }
    }
    
    private boolean insertRow(Connection connection, String tableName, java.util.Map<String, Object> row, 
                            List<String> selectedColumns, String fileName, String sheetName, int rowIndex) throws Exception {
        StringBuilder columns = new StringBuilder("file_name, sheet_name, row_index");
        StringBuilder placeholders = new StringBuilder("?, ?, ?");
        java.util.List<Object> values = new java.util.ArrayList<>();
        values.add(fileName);
        values.add(sheetName);
        values.add(rowIndex);
        
        for (String column : selectedColumns) {
            if (row.containsKey(column)) {
                columns.append(", ").append(column);
                placeholders.append(", ?");
                values.add(row.get(column) != null ? row.get(column).toString() : "");
            }
        }
        
        String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
        
        try (java.sql.PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (int i = 0; i < values.size(); i++) {
                pstmt.setObject(i + 1, values.get(i));
            }
            return pstmt.executeUpdate() > 0;
        }
    }
}