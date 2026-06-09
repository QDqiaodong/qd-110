package com.habit.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class CheckinDTO {
    
    private Long habitId;
    
    private LocalDate date;
}
