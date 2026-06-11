package com.habit.controller;

import com.habit.dto.Result;
import com.habit.entity.MonthlyStat;
import com.habit.service.MonthlyStatService;
import com.habit.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
public class StatsController {
    
    @Autowired
    private StatsService statsService;
    
    @Autowired
    private MonthlyStatService monthlyStatService;
    
    @GetMapping("/week")
    public Result<List<Map<String, Object>>> getWeekStats() {
        List<Map<String, Object>> stats = statsService.getWeekStats();
        return Result.success(stats);
    }
    
    @GetMapping("/month")
    public Result<List<Map<String, Object>>> getMonthStats() {
        List<Map<String, Object>> stats = statsService.getMonthStats();
        return Result.success(stats);
    }
    
    @GetMapping("/monthly-trend")
    public Result<List<Map<String, Object>>> getMonthlyTrend() {
        List<Map<String, Object>> stats = monthlyStatService.getMonthStatsWithTrend();
        return Result.success(stats);
    }
    
    @GetMapping("/monthly/{year}/{month}")
    public Result<MonthlyStat> getMonthlyStat(@PathVariable Integer year, @PathVariable Integer month) {
        MonthlyStat stat = monthlyStatService.getMonthlyStat(year, month);
        return Result.success(stat);
    }
    
    @PostMapping("/monthly/recalculate/{year}/{month}")
    public Result<MonthlyStat> recalculateMonthlyStat(@PathVariable Integer year, @PathVariable Integer month) {
        MonthlyStat stat = monthlyStatService.calculateMonthlyStat(year, month);
        return Result.success(stat);
    }
    
    @PostMapping("/monthly/recalculate/date/{date}")
    public Result<String> recalculateForDate(@PathVariable String date) {
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        monthlyStatService.asyncRecalculateForDate(localDate);
        return Result.success("补算任务已提交");
    }
}
