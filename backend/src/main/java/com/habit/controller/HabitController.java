package com.habit.controller;

import com.habit.dto.HabitDTO;
import com.habit.dto.Result;
import com.habit.entity.Habit;
import com.habit.service.HabitService;
import com.habit.service.StatsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/habits")
public class HabitController {
    
    @Autowired
    private HabitService habitService;
    
    @Autowired
    private StatsService statsService;
    
    @GetMapping
    public Result<List<Habit>> getHabitList() {
        List<Habit> habits = habitService.getHabitList();
        return Result.success(habits);
    }
    
    @PostMapping
    public Result<Habit> createHabit(@Valid @RequestBody HabitDTO dto) {
        Habit habit = habitService.createHabit(dto);
        statsService.clearStatsCache();
        return Result.success(habit);
    }
    
    @PutMapping("/{id}")
    public Result<Habit> updateHabit(@PathVariable Long id, @Valid @RequestBody HabitDTO dto) {
        Habit habit = habitService.updateHabit(id, dto);
        if (habit == null) {
            return Result.error("习惯不存在");
        }
        statsService.clearStatsCache();
        return Result.success(habit);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteHabit(@PathVariable Long id) {
        boolean success = habitService.deleteHabit(id);
        if (!success) {
            return Result.error("删除失败");
        }
        statsService.clearStatsCache();
        return Result.success();
    }
    
    @PostMapping("/{id}/star")
    public Result<Habit> toggleStarred(@PathVariable Long id) {
        Habit habit = habitService.toggleStarred(id);
        if (habit == null) {
            return Result.error("习惯不存在");
        }
        statsService.clearStatsCache();
        return Result.success(habit);
    }
    
    @PutMapping("/starred/order")
    public Result<Void> updateStarredOrder(@RequestBody Map<String, List<Long>> body) {
        List<Long> habitIds = body.get("habitIds");
        boolean success = habitService.updateStarredOrder(habitIds);
        if (!success) {
            return Result.error("更新失败");
        }
        statsService.clearStatsCache();
        return Result.success();
    }
}
