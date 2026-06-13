package com.habit.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class HabitGapRuleDTO {

    @NotNull(message = "习惯ID不能为空")
    private Long habitId;

    private String timeSlot;

    @Min(value = 1, message = "空窗天数至少为1天")
    private Integer gapDays;

    private Boolean enabled;
}
