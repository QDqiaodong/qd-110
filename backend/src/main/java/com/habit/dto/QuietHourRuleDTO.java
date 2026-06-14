package com.habit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class QuietHourRuleDTO {

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    private Boolean enabled;

    private String category;

    private Integer sortOrder;
}
