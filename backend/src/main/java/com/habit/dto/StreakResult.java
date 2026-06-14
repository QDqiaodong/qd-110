package com.habit.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class StreakResult {

    private Long habitId;

    private Integer currentStreak = 0;

    private Integer maxStreak = 0;

    private LocalDate currentStreakStartDate;

    private LocalDate currentStreakEndDate;

    private LocalDate maxStreakStartDate;

    private LocalDate maxStreakEndDate;

    private Integer totalCompletedDays = 0;

    private LocalDate firstCheckinDate;

    private LocalDate lastCheckinDate;

    private List<StreakSegment> segments = new ArrayList<>();

    private List<StreakBreakPoint> breakPoints = new ArrayList<>();

    private Integer totalSegments = 0;

    private Integer totalBreakPoints = 0;
}
