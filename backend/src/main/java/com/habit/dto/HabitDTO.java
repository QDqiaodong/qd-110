package com.habit.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HabitDTO {
    
    @NotBlank(message = "习惯名称不能为空")
    private String name;
    
    private String category;
    
    private String time;
    
    private Boolean remind;
    
    private String color;
}
