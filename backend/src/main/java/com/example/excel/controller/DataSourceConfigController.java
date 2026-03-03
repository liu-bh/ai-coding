package com.example.excel.controller;

import com.example.excel.entity.DataSourceConfig;
import com.example.excel.service.DataSourceConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/datasource")
@CrossOrigin(origins = "*")
public class DataSourceConfigController {
    
    @Autowired
    private DataSourceConfigService dataSourceConfigService;
    
    @GetMapping("/list")
    public ResponseEntity<?> getAllDataSources() {
        try {
            List<DataSourceConfig> dataSources = dataSourceConfigService.getAllActiveDataSources();
            return ResponseEntity.ok(dataSources);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取数据源列表失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> getDataSourceById(@PathVariable Long id) {
        try {
            Optional<DataSourceConfig> dataSource = dataSourceConfigService.getDataSourceById(id);
            if (dataSource.isPresent()) {
                return ResponseEntity.ok(dataSource.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取数据源失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/create")
    public ResponseEntity<?> createDataSource(@RequestBody DataSourceConfig dataSourceConfig) {
        try {
            // 检查名称是否已存在
            Optional<DataSourceConfig> existing = dataSourceConfigService.getDataSourceByName(dataSourceConfig.getName());
            if (existing.isPresent()) {
                return ResponseEntity.badRequest().body("数据源名称已存在");
            }
            
            DataSourceConfig saved = dataSourceConfigService.saveDataSource(dataSourceConfig);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("创建数据源失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDataSource(@PathVariable Long id, @RequestBody DataSourceConfig dataSourceConfig) {
        try {
            Optional<DataSourceConfig> existing = dataSourceConfigService.getDataSourceById(id);
            if (!existing.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            dataSourceConfig.setId(id);
            DataSourceConfig updated = dataSourceConfigService.saveDataSource(dataSourceConfig);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新数据源失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDataSource(@PathVariable Long id) {
        try {
            Optional<DataSourceConfig> existing = dataSourceConfigService.getDataSourceById(id);
            if (!existing.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            dataSourceConfigService.deleteDataSource(id);
            return ResponseEntity.ok().body("数据源删除成功");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除数据源失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/test")
    public ResponseEntity<?> testConnection(@RequestBody DataSourceConfig dataSourceConfig) {
        try {
            boolean isConnected = dataSourceConfigService.testConnection(dataSourceConfig);
            Map<String, Object> response = Map.of(
                "success", isConnected,
                "message", isConnected ? "连接成功" : "连接失败"
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("测试连接失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/save-data")
    public ResponseEntity<?> saveDataToDataSource(@RequestBody Map<String, Object> request) {
        try {
            Long dataSourceId = Long.valueOf(request.get("dataSourceId").toString());
            String fileName = (String) request.get("fileName");
            String sheetName = (String) request.get("sheetName");
            List<Map<String, Object>> data = (List<Map<String, Object>>) request.get("data");
            List<String> selectedColumns = (List<String>) request.get("selectedColumns");
            
            Optional<DataSourceConfig> dataSourceOpt = dataSourceConfigService.getDataSourceById(dataSourceId);
            if (!dataSourceOpt.isPresent()) {
                return ResponseEntity.badRequest().body("数据源不存在");
            }
            
            boolean success = dataSourceConfigService.saveDataToDataSource(
                dataSourceOpt.get(), fileName, sheetName, data, selectedColumns);
            
            Map<String, Object> response = Map.of(
                "success", success,
                "message", success ? "数据保存成功" : "数据保存失败",
                "dataSourceName", dataSourceOpt.get().getName()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("保存数据失败: " + e.getMessage());
        }
    }
}