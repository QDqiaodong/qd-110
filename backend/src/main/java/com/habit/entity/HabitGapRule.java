package com.habit.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("habit_gap_rule")
public class HabitGapRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long habitId;

    private String timeSlot;

    private Integer gapDays;

    private Boolean enabled;

    private Boolean highRisk;

    private Integer currentGapDays;

    private String missedTimeSlotStats;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
