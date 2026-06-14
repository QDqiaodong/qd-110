package com.habit.dto;

import lombok.Data;

@Data
public class ScheduleDTO {
    
    private String name;
    
    private String items;
    
    private String weekdayItems;
    
    private String weekendItems;
    
    private Integer version;
    
    private Long parentId;
    
    private String tag;
    
    private String versionNote;
}
