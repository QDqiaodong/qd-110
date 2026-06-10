package com.habit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("habit_milestone")
public class HabitMilestone {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long habitId;
    
    private String habitName;
    
    private String habitColor;
    
    private Integer milestoneType;
    
    private String milestoneLabel;
    
    private String milestoneIcon;
    
    private LocalDate achieveDate;
    
    private Integer totalCheckins;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
