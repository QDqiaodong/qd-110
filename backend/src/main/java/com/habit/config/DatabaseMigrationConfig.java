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
            migrateHabitGapRuleTable();
            
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

    private void migrateHabitGapRuleTable() {
        String databaseName = jdbcTemplate.queryForObject(
            "SELECT DATABASE()", String.class);
        
        if (databaseName == null) {
            log.warn("未获取到当前数据库名，跳过habit_gap_rule表迁移");
            return;
        }

        String tableCheckSql = "SELECT COUNT(*) FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'habit_gap_rule'";
        
        Integer tableCount = jdbcTemplate.queryForObject(tableCheckSql, Integer.class, databaseName, "habit_gap_rule");
        
        if (tableCount == null || tableCount == 0) {
            String createTableSql = "CREATE TABLE habit_gap_rule (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID'," +
                "habit_id BIGINT NOT NULL COMMENT '习惯ID'," +
                "time_slot VARCHAR(20) DEFAULT '全天' COMMENT '检测时段：早晨/上午/中午/下午/傍晚/晚上/凌晨/全天'," +
                "gap_days INT DEFAULT 3 COMMENT '空窗天数阈值'," +
                "enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用'," +
                "high_risk TINYINT(1) DEFAULT 0 COMMENT '是否高风险'," +
                "current_gap_days INT DEFAULT 0 COMMENT '当前连续空窗天数'," +
                "missed_time_slot_stats TEXT COMMENT '漏做时段统计JSON'," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'," +
                "deleted INT DEFAULT 0 COMMENT '逻辑删除标记'," +
                "INDEX idx_habit_id (habit_id)," +
                "INDEX idx_high_risk (high_risk)," +
                "INDEX idx_enabled (enabled)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='习惯打卡空窗提醒规则'";
            
            jdbcTemplate.execute(createTableSql);
            log.info("创建表成功: habit_gap_rule");
        } else {
            log.debug("表已存在: habit_gap_rule");
            
            addColumnIfNotExists(databaseName, "habit_gap_rule", "habit_id", 
                "BIGINT NOT NULL COMMENT '习惯ID'");
            addColumnIfNotExists(databaseName, "habit_gap_rule", "time_slot", 
                "VARCHAR(20) DEFAULT '全天' COMMENT '检测时段'");
            addColumnIfNotExists(databaseName, "habit_gap_rule", "gap_days", 
                "INT DEFAULT 3 COMMENT '空窗天数阈值'");
            addColumnIfNotExists(databaseName, "habit_gap_rule", "enabled", 
                "TINYINT(1) DEFAULT 1 COMMENT '是否启用'");
            addColumnIfNotExists(databaseName, "habit_gap_rule", "high_risk", 
                "TINYINT(1) DEFAULT 0 COMMENT '是否高风险'");
            addColumnIfNotExists(databaseName, "habit_gap_rule", "current_gap_days", 
                "INT DEFAULT 0 COMMENT '当前连续空窗天数'");
            addColumnIfNotExists(databaseName, "habit_gap_rule", "missed_time_slot_stats", 
                "TEXT COMMENT '漏做时段统计JSON'");
            addColumnIfNotExists(databaseName, "habit_gap_rule", "create_time", 
                "DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'");
            addColumnIfNotExists(databaseName, "habit_gap_rule", "update_time", 
                "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
            addColumnIfNotExists(databaseName, "habit_gap_rule", "deleted", 
                "INT DEFAULT 0 COMMENT '逻辑删除标记'");
            
            addIndexIfNotExists(databaseName, "habit_gap_rule", "idx_habit_id", "idx_habit_id (habit_id)");
            addIndexIfNotExists(databaseName, "habit_gap_rule", "idx_high_risk", "idx_high_risk (high_risk)");
            addIndexIfNotExists(databaseName, "habit_gap_rule", "idx_enabled", "idx_enabled (enabled)");
        }
    }
}
