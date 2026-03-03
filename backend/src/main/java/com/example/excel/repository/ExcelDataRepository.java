package com.example.excel.repository;

import com.example.excel.entity.ExcelData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExcelDataRepository extends JpaRepository<ExcelData, Long> {
    List<ExcelData> findByFileName(String fileName);
    List<ExcelData> findByFileNameAndSheetName(String fileName, String sheetName);
    List<ExcelData> findByIsSelectedTrue();
    void deleteByFileName(String fileName);
}