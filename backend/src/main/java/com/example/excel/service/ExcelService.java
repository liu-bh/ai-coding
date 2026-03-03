package com.example.excel.service;

import com.example.excel.dto.ExcelDataDto;
import com.example.excel.entity.ExcelData;
import com.example.excel.repository.ExcelDataRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExcelService {
    
    @Autowired
    private ExcelDataRepository excelDataRepository;
    
    @Value("${file.upload-dir}")
    private String uploadDir;

    public ExcelDataDto parseExcel(MultipartFile file, int headerEndRow) throws IOException {
        String fileName = file.getOriginalFilename();
        ExcelDataDto excelDataDto = new ExcelDataDto();
        excelDataDto.setFileName(fileName);
        
        List<String> sheetNames = new ArrayList<>();
        Map<String, List<Map<String, Object>>> sheetsData = new HashMap<>();
        Map<String, List<String>> sheetHeaders = new HashMap<>();
        
        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            Sheet sheet = workbook.getSheetAt(i);
            String sheetName = sheet.getSheetName();
            sheetNames.add(sheetName);
            
            List<String> headers = new ArrayList<>();
            List<Map<String, Object>> rowData = new ArrayList<>();
            
            // 读取表头行（前headerEndRow行）
            int maxColNum = 0;
            for (int rowNum = 0; rowNum < headerEndRow; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row != null) {
                    int lastCellNum = row.getLastCellNum();
                    if (lastCellNum > maxColNum) {
                        maxColNum = lastCellNum;
                    }
                }
            }
            
            // 初始化表头列表
            for (int col = 0; col < maxColNum; col++) {
                headers.add("");
            }
            
            // 合并多行表头
            for (int rowNum = 0; rowNum < headerEndRow; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row != null) {
                    for (int col = 0; col < maxColNum; col++) {
                        Cell cell = row.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        String cellValue = getCellValueAsString(cell).trim();
                        String currentHeader = headers.get(col);
                        if (!cellValue.isEmpty()) {
                            if (currentHeader.isEmpty()) {
                                headers.set(col, cellValue);
                            } else {
                                headers.set(col, currentHeader + "_" + cellValue);
                            }
                        }
                    }
                }
            }
            
            // 处理空表头
            for (int col = 0; col < headers.size(); col++) {
                if (headers.get(col).isEmpty()) {
                    headers.set(col, "列" + (col + 1));
                }
            }
            
            sheetHeaders.put(sheetName, headers);
            
            // 读取数据行（从headerEndRow行开始）
            int lastRowNum = sheet.getLastRowNum();
            for (int rowNum = headerEndRow; rowNum <= lastRowNum; rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row != null) {
                    Map<String, Object> rowMap = new LinkedHashMap<>();
                    
                    for (int col = 0; col < headers.size(); col++) {
                        Cell cell = row.getCell(col, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                        String cellValue = getCellValueAsString(cell);
                        rowMap.put(headers.get(col), cellValue);
                    }
                    
                    rowData.add(rowMap);
                }
            }
            
            sheetsData.put(sheetName, rowData);
        }
        
        workbook.close();
        
        excelDataDto.setSheetNames(sheetNames);
        excelDataDto.setSheetsData(sheetsData);
        excelDataDto.setSheetHeaders(sheetHeaders);
        
        return excelDataDto;
    }

    public void saveToDatabase(String fileName, String sheetName, List<Map<String, Object>> data, List<String> selectedColumns, String databaseType) {
        for (int i = 0; i < data.size(); i++) {
            Map<String, Object> row = data.get(i);
            for (String column : selectedColumns) {
                if (row.containsKey(column)) {
                    ExcelData excelData = new ExcelData();
                    excelData.setFileName(fileName);
                    excelData.setSheetName(sheetName);
                    excelData.setRowIndex(i);
                    excelData.setColumnName(column);
                    excelData.setColumnValue(String.valueOf(row.get(column)));
                    excelData.setDataType(getDataType(row.get(column)));
                    excelData.setDatabaseType(databaseType);
                    excelData.setIsSelected(true);
                    excelDataRepository.save(excelData);
                }
            }
        }
    }

    public byte[] exportExcel(ExcelDataDto excelDataDto) throws IOException {
        System.out.println("=== 导出Excel调试信息 ===");
        System.out.println("Sheet数量: " + excelDataDto.getSheetNames().size());
        
        Workbook workbook = new XSSFWorkbook();
        
        for (String sheetName : excelDataDto.getSheetNames()) {
            Sheet sheet = workbook.createSheet(sheetName);
            List<String> headers = excelDataDto.getSheetHeaders().get(sheetName);
            List<Map<String, Object>> data = excelDataDto.getSheetsData().get(sheetName);
            
            System.out.println("Sheet: " + sheetName);
            System.out.println("Headers: " + headers);
            System.out.println("Data rows: " + (data != null ? data.size() : 0));
            
            if (data != null && data.size() > 0) {
                System.out.println("第一行数据: " + data.get(0));
            }
            
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                String headerValue = headers.get(i);
                if (headerValue == null) {
                    headerValue = "";
                }
                cell.setCellValue(headerValue);
                System.out.println("设置表头单元格 " + i + ": " + headerValue);
            }
            
            if (data != null) {
                for (int i = 0; i < data.size(); i++) {
                    Row row = sheet.createRow(i + 1);
                    Map<String, Object> rowData = data.get(i);
                    
                    for (int j = 0; j < headers.size(); j++) {
                        Cell cell = row.createCell(j);
                        String headerKey = headers.get(j);
                        Object value = rowData.get(headerKey);
                        
                        if (value != null) {
                            String strValue;
                            if (value instanceof Map || value instanceof List) {
                                strValue = value.toString();
                                System.out.println("警告: 第" + (i+1) + "行列'" + headerKey + "'的值是复杂对象: " + strValue);
                            } else {
                                strValue = String.valueOf(value);
                            }
                            cell.setCellValue(strValue);
                            System.out.println("设置数据单元格 " + i + "-" + j + "(" + headerKey + "): " + strValue);
                        }
                    }
                }
            }
        }
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        byte[] result = outputStream.toByteArray();
        System.out.println("=== 导出完成 ===");
        System.out.println("生成的Excel文件大小: " + result.length + " 字节");
        
        return result;
    }

    public List<ExcelData> getSelectedData() {
        return excelDataRepository.findByIsSelectedTrue();
    }

    public void deleteDataByFileName(String fileName) {
        excelDataRepository.deleteByFileName(fileName);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    private String getDataType(Object value) {
        if (value == null) {
            return "NULL";
        }
        
        if (value instanceof Number) {
            return "NUMBER";
        } else if (value instanceof Boolean) {
            return "BOOLEAN";
        } else {
            return "STRING";
        }
    }
}