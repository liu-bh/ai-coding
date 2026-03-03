package com.example.excel.controller;

import com.example.excel.dto.ExcelDataDto;
import com.example.excel.entity.ExcelData;
import com.example.excel.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/excel")
@CrossOrigin(origins = "*")
public class ExcelController {
    
    @Autowired
    private ExcelService excelService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "headerEndRow", required = false, defaultValue = "1") Integer headerEndRow) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("请选择要上传的文件");
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls"))) {
                return ResponseEntity.badRequest().body("只支持Excel文件格式(.xlsx或.xls)");
            }
            
            ExcelDataDto excelDataDto = excelService.parseExcel(file, headerEndRow);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "文件上传成功");
            response.put("data", excelDataDto);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("文件处理失败: " + e.getMessage());
        }
    }

    @PostMapping("/save")
    public ResponseEntity<?> saveToDatabase(@RequestBody Map<String, Object> request) {
        try {
            String fileName = (String) request.get("fileName");
            String sheetName = (String) request.get("sheetName");
            List<Map<String, Object>> data = (List<Map<String, Object>>) request.get("data");
            List<String> selectedColumns = (List<String>) request.get("selectedColumns");
            String databaseType = (String) request.get("databaseType");
            
            if (databaseType == null || databaseType.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("数据库类型不能为空");
            }
            
            excelService.saveToDatabase(fileName, sheetName, data, selectedColumns, databaseType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "数据保存成功");
            response.put("databaseType", databaseType);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("数据保存失败: " + e.getMessage());
        }
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportExcel(@RequestBody ExcelDataDto excelDataDto) {
        try {
            byte[] excelContent = excelService.exportExcel(excelDataDto);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "export.xlsx");
            headers.setContentLength(excelContent.length);
            
            return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/export-selected")
    public ResponseEntity<byte[]> exportSelectedData(@RequestBody ExcelDataDto excelDataDto) {
        try {
            byte[] excelContent = excelService.exportExcel(excelDataDto);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "export_selected.xlsx");
            headers.setContentLength(excelContent.length);
            
            return new ResponseEntity<>(excelContent, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/selected-data")
    public ResponseEntity<?> getSelectedData() {
        try {
            List<ExcelData> selectedData = excelService.getSelectedData();
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "查询成功");
            response.put("data", selectedData);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("查询失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<?> deleteDataByFileName(@PathVariable String fileName) {
        try {
            excelService.deleteDataByFileName(fileName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "数据删除成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除失败: " + e.getMessage());
        }
    }
}