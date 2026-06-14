package com.habit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("quiet_hour_rule")
public class QuietHourRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String startTime;

    private String endTime;

    private Boolean enabled;

    private String category;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
