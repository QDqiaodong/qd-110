package com.habit.dto;

import lombok.Data;

@Data
public class MorningCardDTO {
    
    private Long id;
    
    private String name;
    
    private String category;
    
    private String time;
    
    private String color;
    
    private Boolean completed;
    
    private String icon;
}
