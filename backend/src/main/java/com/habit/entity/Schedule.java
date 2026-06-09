package com.habit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("schedule")
public class Schedule {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String items;
    
    private Boolean isCustom;
    
    private Boolean isCurrent;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
