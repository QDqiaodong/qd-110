CREATE DATABASE IF NOT EXISTS habit_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE habit_db;

CREATE TABLE IF NOT EXISTS habit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '习惯名称',
    category VARCHAR(50) DEFAULT '生活' COMMENT '分类',
    time VARCHAR(20) COMMENT '执行时间',
    remind TINYINT(1) DEFAULT 0 COMMENT '是否提醒',
    color VARCHAR(20) DEFAULT '#3b82f6' COMMENT '颜色',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_category (category),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='习惯表';

CREATE TABLE IF NOT EXISTS checkin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    habit_id BIGINT NOT NULL COMMENT '习惯ID',
    checkin_date DATE NOT NULL COMMENT '打卡日期',
    completed TINYINT(1) DEFAULT 1 COMMENT '是否完成',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_habit_id (habit_id),
    INDEX idx_checkin_date (checkin_date),
    UNIQUE KEY uk_habit_date (habit_id, checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='打卡记录表';

CREATE TABLE IF NOT EXISTS schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    items TEXT COMMENT '时间项JSON',
    is_custom TINYINT(1) DEFAULT 0 COMMENT '是否自定义',
    is_current TINYINT(1) DEFAULT 0 COMMENT '是否当前使用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_is_current (is_current)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='作息模板表';

INSERT INTO habit (name, category, time, remind, color) VALUES
('早起', '作息', '07:00', 1, '#3b82f6'),
('阅读30分钟', '学习', '20:00', 1, '#10b981'),
('运动锻炼', '健康', '18:00', 0, '#f59e0b'),
('喝8杯水', '健康', NULL, 0, '#06b6d4');
