package com.habit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreakBreakPoint {

    private LocalDate breakDate;

    private Integer gapDays;

    private LocalDate previousCheckinDate;

    private LocalDate nextCheckinDate;
}
