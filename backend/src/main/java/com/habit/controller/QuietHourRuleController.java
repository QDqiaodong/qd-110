package com.habit.controller;

import com.habit.dto.QuietHourRuleDTO;
import com.habit.dto.Result;
import com.habit.entity.Habit;
import com.habit.entity.QuietHourRule;
import com.habit.service.HabitService;
import com.habit.service.QuietHourRuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/quiet-hours")
public class QuietHourRuleController {

    @Autowired
    private QuietHourRuleService quietHourRuleService;

    @Autowired
    private HabitService habitService;

    @GetMapping
    public Result<List<QuietHourRule>> getAllRules() {
        List<QuietHourRule> rules = quietHourRuleService.getAllRules();
        return Result.success(rules);
    }

    @GetMapping("/enabled")
    public Result<List<QuietHourRule>> getEnabledRules() {
        List<QuietHourRule> rules = quietHourRuleService.getEnabledRules();
        return Result.success(rules);
    }

    @GetMapping("/check/{time}")
    public Result<Map<String, Object>> checkTimeInQuietHour(@PathVariable String time) {
        boolean inQuiet = quietHourRuleService.isTimeInQuietHour(time);
        QuietHourRule matched = quietHourRuleService.getMatchingQuietHour(time);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("time", time);
        result.put("inQuietHour", inQuiet);
        result.put("matchedRule", matched);
        return Result.success(result);
    }

    @GetMapping("/reminder-schedule")
    public Result<Map<String, Object>> getReminderSchedule() {
        List<Habit> habits = habitService.getHabitList();
        Map<String, Object> schedule = quietHourRuleService.getReminderSchedule(habits);
        return Result.success(schedule);
    }

    @PostMapping
    public Result<QuietHourRule> createRule(@Valid @RequestBody QuietHourRuleDTO dto) {
        try {
            QuietHourRule rule = quietHourRuleService.createRule(dto);
            return Result.success(rule);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<QuietHourRule> updateRule(@PathVariable Long id, @Valid @RequestBody QuietHourRuleDTO dto) {
        try {
            QuietHourRule rule = quietHourRuleService.updateRule(id, dto);
            if (rule == null) {
                return Result.error("规则不存在");
            }
            return Result.success(rule);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        boolean success = quietHourRuleService.deleteRule(id);
        if (!success) {
            return Result.error("删除失败");
        }
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<QuietHourRule> toggleRule(@PathVariable Long id) {
        QuietHourRule rule = quietHourRuleService.toggleRule(id);
        if (rule == null) {
            return Result.error("规则不存在");
        }
        return Result.success(rule);
    }
}
