package com.habit.controller;

import com.habit.dto.Result;
import com.habit.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
public class StatsController {
    
    @Autowired
    private StatsService statsService;
    
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
}
