package com.habit.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseMigrationConfig {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseMigrationConfig.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @PostConstruct
    public void migrate() {
        try {
            log.info("开始检查数据库迁移...");
            
            migrateHabitTable();
            
            log.info("数据库迁移检查完成");
        } catch (Exception e) {
            log.error("数据库迁移失败", e);
        }
    }
    
    private void migrateHabitTable() {
        String databaseName = jdbcTemplate.queryForObject(
            "SELECT DATABASE()", String.class);
        
        if (databaseName == null) {
            log.warn("未获取到当前数据库名，跳过迁移");
            return;
        }
        
        addColumnIfNotExists(databaseName, "habit", "starred", 
            "TINYINT(1) DEFAULT 0 COMMENT '是否星标'");
        
        addColumnIfNotExists(databaseName, "habit", "sort_order", 
            "INT DEFAULT 0 COMMENT '排序号'");
        
        addIndexIfNotExists(databaseName, "habit", "idx_starred", "idx_starred (starred)");
        
        updateDefaultStarredHabits();
    }
    
    private void addColumnIfNotExists(String dbName, String tableName, String columnName, String columnDefinition) {
        String checkSql = "SELECT COUNT(*) FROM information_schema.COLUMNS " +
            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, dbName, tableName, columnName);
        
        if (count == null || count == 0) {
            String alterSql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition;
            jdbcTemplate.execute(alterSql);
            log.info("添加字段成功: {}.{} = {}", tableName, columnName, columnDefinition);
        } else {
            log.debug("字段已存在: {}.{}", tableName, columnName);
        }
    }
    
    private void addIndexIfNotExists(String dbName, String tableName, String indexName, String indexDefinition) {
        String checkSql = "SELECT COUNT(*) FROM information_schema.STATISTICS " +
            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND INDEX_NAME = ?";
        
        Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, dbName, tableName, indexName);
        
        if (count == null || count == 0) {
            String alterSql = "ALTER TABLE " + tableName + " ADD INDEX " + indexDefinition;
            jdbcTemplate.execute(alterSql);
            log.info("添加索引成功: {}.{}", tableName, indexName);
        } else {
            log.debug("索引已存在: {}.{}", tableName, indexName);
        }
    }
    
    private void updateDefaultStarredHabits() {
        try {
            String countSql = "SELECT COUNT(*) FROM habit WHERE starred = 1";
            Integer starredCount = jdbcTemplate.queryForObject(countSql, Integer.class);
            
            if (starredCount != null && starredCount == 0) {
                String totalSql = "SELECT COUNT(*) FROM habit";
                Integer totalCount = jdbcTemplate.queryForObject(totalSql, Integer.class);
                
                if (totalCount != null && totalCount > 0) {
                    String updateSql = "UPDATE habit SET starred = 1, sort_order = id " +
                        "WHERE name IN ('早起', '喝8杯水') AND starred = 0";
                    int updated = jdbcTemplate.update(updateSql);
                    if (updated > 0) {
                        log.info("初始化默认星标习惯成功，更新了 {} 条记录", updated);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("初始化默认星标习惯失败（可能数据已存在）: {}", e.getMessage());
        }
    }
}
