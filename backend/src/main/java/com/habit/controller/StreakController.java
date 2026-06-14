package com.habit.controller;

import com.habit.dto.Result;
import com.habit.dto.StreakResult;
import com.habit.service.StreakCalculatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/streaks")
public class StreakController {

    @Autowired
    private StreakCalculatorService streakCalculatorService;

    @GetMapping("/habit/{habitId}")
    public Result<StreakResult> getHabitStreak(@PathVariable Long habitId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        StreakResult result;
        if (startDate != null || endDate != null) {
            result = streakCalculatorService.calculateForHabit(habitId, startDate, endDate);
        } else {
            result = streakCalculatorService.calculateForHabit(habitId);
        }
        return Result.success(result);
    }

    @GetMapping("/habit/{habitId}/archived")
    public Result<StreakResult> getArchivedHabitStreak(@PathVariable Long habitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate archiveDate) {
        StreakResult result = streakCalculatorService.calculateForHabitUntil(habitId, archiveDate.plusDays(1));
        return Result.success(result);
    }

    @GetMapping("/habit/{habitId}/range")
    public Result<StreakResult> getHabitStreakInRange(@PathVariable Long habitId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rangeStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rangeEnd) {
        StreakResult result = streakCalculatorService.calculateForRange(habitId, rangeStart, rangeEnd.plusDays(1));
        return Result.success(result);
    }

    @PostMapping("/batch")
    public Result<Map<Long, StreakResult>> getBatchStreaks(@RequestBody List<Long> habitIds) {
        Map<Long, StreakResult> results = streakCalculatorService.calculateBatch(habitIds);
        return Result.success(results);
    }

    @PostMapping("/clear/{habitId}")
    public Result<String> clearStreakCache(@PathVariable Long habitId) {
        streakCalculatorService.clearStreakCache(habitId);
        return Result.success("已清理习惯 streak 缓存");
    }

    @PostMapping("/clear-all")
    public Result<String> clearAllStreakCache() {
        streakCalculatorService.clearAllStreakCache();
        return Result.success("已清理所有 streak 缓存");
    }
}
