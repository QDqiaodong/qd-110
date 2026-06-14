CREATE DATABASE IF NOT EXISTS habit_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE habit_db;

CREATE TABLE IF NOT EXISTS habit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '习惯名称',
    category VARCHAR(50) DEFAULT '生活' COMMENT '分类',
    time VARCHAR(20) COMMENT '执行时间',
    remind TINYINT(1) DEFAULT 0 COMMENT '是否提醒',
    color VARCHAR(20) DEFAULT '#3b82f6' COMMENT '颜色',
    starred TINYINT(1) DEFAULT 0 COMMENT '是否星标',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    archived TINYINT(1) DEFAULT 0 COMMENT '是否归档',
    archive_time DATETIME DEFAULT NULL COMMENT '归档时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_category (category),
    INDEX idx_starred (starred),
    INDEX idx_archived (archived),
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

CREATE TABLE IF NOT EXISTS challenge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    habit_id BIGINT NOT NULL COMMENT '习惯ID',
    habit_name VARCHAR(100) NOT NULL COMMENT '习惯名称',
    habit_color VARCHAR(20) DEFAULT '#3b82f6' COMMENT '习惯颜色',
    start_date DATE NOT NULL COMMENT '开始日期',
    end_date DATE NOT NULL COMMENT '结束日期',
    total_days INT DEFAULT 21 COMMENT '总天数',
    completed_days INT DEFAULT 0 COMMENT '已完成天数',
    current_streak INT DEFAULT 0 COMMENT '当前连续天数',
    max_streak INT DEFAULT 0 COMMENT '最大连续天数',
    status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active/completed/given_up',
    milestone_7 TINYINT(1) DEFAULT 0 COMMENT '7天里程碑',
    milestone_14 TINYINT(1) DEFAULT 0 COMMENT '14天里程碑',
    milestone_21 TINYINT(1) DEFAULT 0 COMMENT '21天里程碑',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_habit_id (habit_id),
    INDEX idx_status (status),
    INDEX idx_start_date (start_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='21天挑战表';

CREATE TABLE IF NOT EXISTS habit_milestone (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    habit_id BIGINT NOT NULL COMMENT '习惯ID',
    habit_name VARCHAR(100) NOT NULL COMMENT '习惯名称',
    habit_color VARCHAR(20) DEFAULT '#3b82f6' COMMENT '习惯颜色',
    milestone_type INT NOT NULL COMMENT '里程碑类型: 7/30/100/365',
    milestone_label VARCHAR(50) NOT NULL COMMENT '里程碑标签',
    milestone_icon VARCHAR(20) DEFAULT '🎉' COMMENT '里程碑图标',
    achieve_date DATE NOT NULL COMMENT '达成日期',
    total_checkins INT DEFAULT 0 COMMENT '累计打卡次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    INDEX idx_habit_id (habit_id),
    INDEX idx_milestone_type (milestone_type),
    INDEX idx_achieve_date (achieve_date),
    UNIQUE KEY uk_habit_milestone (habit_id, milestone_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='习惯里程碑表';

CREATE TABLE IF NOT EXISTS monthly_stat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stat_year INT NOT NULL COMMENT '统计年份',
    stat_month INT NOT NULL COMMENT '统计月份',
    total_habits INT DEFAULT 0 COMMENT '习惯总数',
    total_checkin_days INT DEFAULT 0 COMMENT '总打卡天数',
    avg_completed_per_day INT DEFAULT 0 COMMENT '日均完成数',
    completion_rate INT DEFAULT 0 COMMENT '完成率(%)',
    trend_data TEXT COMMENT '趋势数据JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_year_month (stat_year, stat_month),
    INDEX idx_stat_year (stat_year),
    INDEX idx_stat_month (stat_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月度统计表';

CREATE TABLE IF NOT EXISTS habit_gap_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    habit_id BIGINT NOT NULL COMMENT '习惯ID',
    time_slot VARCHAR(20) DEFAULT '全天' COMMENT '检测时段：早晨/上午/中午/下午/傍晚/晚上/凌晨/全天',
    gap_days INT DEFAULT 3 COMMENT '空窗天数阈值',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    high_risk TINYINT(1) DEFAULT 0 COMMENT '是否高风险',
    current_gap_days INT DEFAULT 0 COMMENT '当前连续空窗天数',
    missed_time_slot_stats TEXT COMMENT '漏做时段统计JSON',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_habit_id (habit_id),
    INDEX idx_high_risk (high_risk),
    INDEX idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='习惯打卡空窗提醒规则';

CREATE TABLE IF NOT EXISTS quiet_hour_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    start_time VARCHAR(10) NOT NULL COMMENT '开始时间 HH:mm',
    end_time VARCHAR(10) NOT NULL COMMENT '结束时间 HH:mm',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    category VARCHAR(50) DEFAULT '自定义' COMMENT '分类：作息/专注/自定义',
    sort_order INT DEFAULT 0 COMMENT '排序号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_enabled (enabled),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安静时段规则表';

INSERT INTO habit (name, category, time, remind, color, starred, sort_order) VALUES
('早起', '作息', '07:00', 1, '#3b82f6', 1, 1),
('阅读30分钟', '学习', '20:00', 1, '#10b981', 0, 1),
('运动锻炼', '健康', '18:00', 0, '#f59e0b', 0, 2),
('喝8杯水', '健康', NULL, 0, '#06b6d4', 1, 2);

INSERT INTO quiet_hour_rule (name, start_time, end_time, enabled, category, sort_order) VALUES
('午休时段', '12:30', '14:00', 1, '作息', 1),
('深夜勿扰', '23:00', '06:00', 1, '作息', 2),
('深度工作', '09:30', '11:30', 0, '专注', 3),
('下午专注', '15:00', '17:00', 0, '专注', 4);
