package com.habit.controller;

import com.habit.dto.HabitGapRuleDTO;
import com.habit.dto.Result;
import com.habit.entity.HabitGapRule;
import com.habit.service.HabitGapRuleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/habit-gap-rules")
public class HabitGapRuleController {

    @Autowired
    private HabitGapRuleService habitGapRuleService;

    @GetMapping("/habit/{habitId}")
    public Result<List<HabitGapRule>> getRulesByHabitId(@PathVariable Long habitId) {
        List<HabitGapRule> rules = habitGapRuleService.getRulesByHabitId(habitId);
        return Result.success(rules);
    }

    @GetMapping("/high-risk")
    public Result<List<HabitGapRule>> getHighRiskRules() {
        List<HabitGapRule> rules = habitGapRuleService.getHighRiskRules();
        return Result.success(rules);
    }

    @GetMapping("/analysis/{habitId}")
    public Result<Map<String, Object>> getGapAnalysis(@PathVariable Long habitId) {
        Map<String, Object> analysis = habitGapRuleService.getGapAnalysis(habitId);
        return Result.success(analysis);
    }

    @PostMapping
    public Result<HabitGapRule> createRule(@Valid @RequestBody HabitGapRuleDTO dto) {
        try {
            HabitGapRule rule = habitGapRuleService.createRule(dto);
            return Result.success(rule);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<HabitGapRule> updateRule(@PathVariable Long id, @Valid @RequestBody HabitGapRuleDTO dto) {
        HabitGapRule rule = habitGapRuleService.updateRule(id, dto);
        if (rule == null) {
            return Result.error("规则不存在");
        }
        return Result.success(rule);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        boolean success = habitGapRuleService.deleteRule(id);
        if (!success) {
            return Result.error("删除失败");
        }
        return Result.success();
    }

    @PostMapping("/{id}/toggle")
    public Result<HabitGapRule> toggleRule(@PathVariable Long id) {
        HabitGapRule rule = habitGapRuleService.toggleRule(id);
        if (rule == null) {
            return Result.error("规则不存在");
        }
        return Result.success(rule);
    }

    @PostMapping("/detect")
    public Result<Void> detectGaps() {
        habitGapRuleService.detectGapsForAllRules();
        return Result.success();
    }

    @PostMapping("/detect/{id}")
    public Result<Void> detectGapForRule(@PathVariable Long id) {
        HabitGapRule rule = habitGapRuleService.getById(id);
        if (rule == null) {
            return Result.error("规则不存在");
        }
        habitGapRuleService.detectGapForRule(rule);
        return Result.success();
    }
}
