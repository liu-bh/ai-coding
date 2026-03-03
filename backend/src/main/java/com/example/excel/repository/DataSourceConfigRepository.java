package com.example.excel.repository;

import com.example.excel.entity.DataSourceConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataSourceConfigRepository extends JpaRepository<DataSourceConfig, Long> {
    
    List<DataSourceConfig> findByIsActive(Boolean isActive);
    
    DataSourceConfig findByName(String name);
    
    DataSourceConfig findByNameAndIsActive(String name, Boolean isActive);
}