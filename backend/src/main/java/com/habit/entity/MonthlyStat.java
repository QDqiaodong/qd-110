package com.habit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("monthly_stat")
public class MonthlyStat {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Integer statYear;
    
    private Integer statMonth;
    
    private Integer totalHabits;
    
    private Integer totalCheckinDays;
    
    private Integer avgCompletedPerDay;
    
    private Integer completionRate;
    
    private String trendData;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
