package com.example.excel.dto;

import java.util.List;
import java.util.Map;

public class ExcelDataDto {
    private String fileName;
    private List<String> sheetNames;
    private Map<String, List<Map<String, Object>>> sheetsData;
    private Map<String, List<String>> sheetHeaders;

    public ExcelDataDto() {
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<String> getSheetNames() {
        return sheetNames;
    }

    public void setSheetNames(List<String> sheetNames) {
        this.sheetNames = sheetNames;
    }

    public Map<String, List<Map<String, Object>>> getSheetsData() {
        return sheetsData;
    }

    public void setSheetsData(Map<String, List<Map<String, Object>>> sheetsData) {
        this.sheetsData = sheetsData;
    }

    public Map<String, List<String>> getSheetHeaders() {
        return sheetHeaders;
    }

    public void setSheetHeaders(Map<String, List<String>> sheetHeaders) {
        this.sheetHeaders = sheetHeaders;
    }
}