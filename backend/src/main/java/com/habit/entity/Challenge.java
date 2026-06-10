package com.habit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("challenge")
public class Challenge {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long habitId;
    
    private String habitName;
    
    private String habitColor;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Integer totalDays;
    
    private Integer completedDays;
    
    private Integer currentStreak;
    
    private Integer maxStreak;
    
    private String status;
    
    private Boolean milestone7;
    
    private Boolean milestone14;
    
    private Boolean milestone21;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
