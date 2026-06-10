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
    
    private Integer totalDays = 21;
    
    private Integer completedDays = 0;
    
    private Integer currentStreak = 0;
    
    private Integer maxStreak = 0;
    
    private String status = "active";
    
    @TableField("milestone_7")
    private Boolean milestone7 = false;
    
    @TableField("milestone_14")
    private Boolean milestone14 = false;
    
    @TableField("milestone_21")
    private Boolean milestone21 = false;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
