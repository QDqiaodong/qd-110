package com.habit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habit.dto.HabitGapRuleDTO;
import com.habit.entity.Checkin;
import com.habit.entity.Habit;
import com.habit.entity.HabitGapRule;
import com.habit.mapper.HabitGapRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class HabitGapRuleService extends ServiceImpl<HabitGapRuleMapper, HabitGapRule> {

    private static final Logger log = LoggerFactory.getLogger(HabitGapRuleService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private HabitService habitService;

    public List<HabitGapRule> getRulesByHabitId(Long habitId) {
        return this.list(new LambdaQueryWrapper<HabitGapRule>()
                .eq(HabitGapRule::getHabitId, habitId)
                .orderByDesc(HabitGapRule::getCreateTime));
    }

    public List<HabitGapRule> getEnabledRules() {
        return this.list(new LambdaQueryWrapper<HabitGapRule>()
                .eq(HabitGapRule::getEnabled, true));
    }

    public List<HabitGapRule> getHighRiskRules() {
        return this.list(new LambdaQueryWrapper<HabitGapRule>()
                .eq(HabitGapRule::getEnabled, true)
                .eq(HabitGapRule::getHighRisk, true)
                .orderByDesc(HabitGapRule::getCurrentGapDays));
    }

    @Transactional
    public HabitGapRule createRule(HabitGapRuleDTO dto) {
        Habit habit = habitService.getById(dto.getHabitId());
        if (habit == null) {
            throw new IllegalArgumentException("习惯不存在");
        }

        HabitGapRule rule = new HabitGapRule();
        rule.setHabitId(dto.getHabitId());
        rule.setTimeSlot(dto.getTimeSlot() != null ? dto.getTimeSlot() : "全天");
        rule.setGapDays(dto.getGapDays() != null ? dto.getGapDays() : 3);
        rule.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        rule.setHighRisk(false);
        rule.setCurrentGapDays(0);
        rule.setMissedTimeSlotStats("{}");

        this.save(rule);
        return rule;
    }

    @Transactional
    public HabitGapRule updateRule(Long id, HabitGapRuleDTO dto) {
        HabitGapRule rule = this.getById(id);
        if (rule == null) {
            return null;
        }

        if (dto.getTimeSlot() != null) {
            rule.setTimeSlot(dto.getTimeSlot());
        }
        if (dto.getGapDays() != null) {
            rule.setGapDays(dto.getGapDays());
        }
        if (dto.getEnabled() != null) {
            rule.setEnabled(dto.getEnabled());
        }

        this.updateById(rule);
        return rule;
    }

    @Transactional
    public boolean deleteRule(Long id) {
        return this.removeById(id);
    }

    @Transactional
    public HabitGapRule toggleRule(Long id) {
        HabitGapRule rule = this.getById(id);
        if (rule == null) {
            return null;
        }
        rule.setEnabled(!(rule.getEnabled() != null && rule.getEnabled()));
        this.updateById(rule);
        return rule;
    }

    @Transactional
    public void detectGapsForAllRules() {
        log.info("开始执行空窗检测任务...");
        List<HabitGapRule> rules = getEnabledRules();
        for (HabitGapRule rule : rules) {
            try {
                detectGapForRule(rule);
            } catch (Exception e) {
                log.error("检测习惯ID {} 的空窗规则失败", rule.getHabitId(), e);
            }
        }
        log.info("空窗检测任务完成，共检测 {} 条规则", rules.size());
    }

    @Transactional
    public void detectGapForRule(HabitGapRule rule) {
        Habit habit = habitService.getById(rule.getHabitId());
        if (habit == null || habit.getArchived()) {
            return;
        }

        int gapDays = rule.getGapDays() != null ? rule.getGapDays() : 3;
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(gapDays + 1);

        List<Checkin> checkins = checkinService.getCheckinsByDateRange(startDate, today)
                .stream()
                .filter(c -> c.getHabitId().equals(rule.getHabitId()))
                .collect(Collectors.toList());

        int currentGap = calculateCurrentGap(checkins, rule.getTimeSlot(), habit.getTime(), today);

        rule.setCurrentGapDays(currentGap);
        rule.setHighRisk(currentGap >= gapDays);

        if (currentGap > 0) {
            updateMissedTimeSlotStats(rule, checkins, habit.getTime(), today);
        }

        this.updateById(rule);

        if (currentGap >= gapDays) {
            log.warn("习惯[{}]检测到高风险漏做！当前空窗{}天，超过阈值{}天。时段：{}",
                    habit.getName(), currentGap, gapDays, rule.getTimeSlot());
        }
    }

    private int calculateCurrentGap(List<Checkin> checkins, String timeSlot, String habitTime, LocalDate today) {
        Set<LocalDate> completedDates = checkins.stream()
                .filter(c -> Boolean.TRUE.equals(c.getCompleted()))
                .filter(c -> isInTimeSlot(c.getCheckinDate(), habitTime, timeSlot))
                .map(Checkin::getCheckinDate)
                .collect(Collectors.toSet());

        int gap = 0;
        LocalDate checkDate = today;

        while (!checkDate.isBefore(today.minusDays(30))) {
            if (isDateRelevant(checkDate, timeSlot, habitTime) && !completedDates.contains(checkDate)) {
                gap++;
            } else if (completedDates.contains(checkDate)) {
                break;
            }
            checkDate = checkDate.minusDays(1);
        }

        return gap;
    }

    private boolean isDateRelevant(LocalDate date, String timeSlot, String habitTime) {
        if ("全天".equals(timeSlot) || timeSlot == null || timeSlot.isEmpty()) {
            return true;
        }
        return isInTimeSlot(date, habitTime, timeSlot);
    }

    private boolean isInTimeSlot(LocalDate date, String habitTime, String timeSlot) {
        if (habitTime == null || habitTime.isEmpty()) {
            return true;
        }

        try {
            int hour = Integer.parseInt(habitTime.split(":")[0]);
            return switch (timeSlot) {
                case "早晨" -> hour >= 4 && hour < 10;
                case "上午" -> hour >= 10 && hour < 12;
                case "中午" -> hour >= 12 && hour < 14;
                case "下午" -> hour >= 14 && hour < 18;
                case "傍晚" -> hour >= 18 && hour < 20;
                case "晚上" -> hour >= 20 && hour < 24;
                case "凌晨" -> hour >= 0 && hour < 4;
                default -> true;
            };
        } catch (Exception e) {
            return true;
        }
    }

    private void updateMissedTimeSlotStats(HabitGapRule rule, List<Checkin> checkins, String habitTime, LocalDate today) {
        try {
            Map<String, Integer> stats;
            if (rule.getMissedTimeSlotStats() != null && !rule.getMissedTimeSlotStats().isEmpty()) {
                stats = objectMapper.readValue(rule.getMissedTimeSlotStats(),
                        new TypeReference<Map<String, Integer>>() {});
            } else {
                stats = new HashMap<>();
            }

            String timeSlot = getTimeSlotFromHabitTime(habitTime);
            stats.merge(timeSlot, 1, Integer::sum);

            rule.setMissedTimeSlotStats(objectMapper.writeValueAsString(stats));
        } catch (Exception e) {
            log.error("更新漏做时段统计失败", e);
        }
    }

    private String getTimeSlotFromHabitTime(String habitTime) {
        if (habitTime == null || habitTime.isEmpty()) {
            return "全天";
        }
        try {
            int hour = Integer.parseInt(habitTime.split(":")[0]);
            if (hour >= 4 && hour < 10) return "早晨";
            if (hour >= 10 && hour < 12) return "上午";
            if (hour >= 12 && hour < 14) return "中午";
            if (hour >= 14 && hour < 18) return "下午";
            if (hour >= 18 && hour < 20) return "傍晚";
            if (hour >= 20 && hour < 24) return "晚上";
            if (hour >= 0 && hour < 4) return "凌晨";
        } catch (Exception ignored) {
        }
        return "全天";
    }

    public Map<String, Object> getGapAnalysis(Long habitId) {
        Map<String, Object> result = new HashMap<>();
        List<HabitGapRule> rules = getRulesByHabitId(habitId);
        result.put("rules", rules);

        Habit habit = habitService.getById(habitId);
        if (habit != null) {
            result.put("habitName", habit.getName());
            result.put("habitTime", habit.getTime());
        }

        boolean hasHighRisk = rules.stream()
                .anyMatch(r -> Boolean.TRUE.equals(r.getHighRisk()));
        result.put("hasHighRisk", hasHighRisk);

        Optional<HabitGapRule> maxGapRule = rules.stream()
                .filter(r -> r.getCurrentGapDays() != null)
                .max(Comparator.comparingInt(HabitGapRule::getCurrentGapDays));
        maxGapRule.ifPresent(habitGapRule -> result.put("maxGapDays", habitGapRule.getCurrentGapDays()));

        Map<String, Integer> combinedStats = new HashMap<>();
        for (HabitGapRule rule : rules) {
            if (rule.getMissedTimeSlotStats() != null && !rule.getMissedTimeSlotStats().isEmpty()) {
                try {
                    Map<String, Integer> stats = objectMapper.readValue(
                            rule.getMissedTimeSlotStats(),
                            new TypeReference<Map<String, Integer>>() {});
                    stats.forEach((k, v) -> combinedStats.merge(k, v, Integer::sum));
                } catch (Exception ignored) {
                }
            }
        }
        result.put("missedTimeSlotStats", combinedStats);

        String weakestSlot = combinedStats.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        result.put("weakestTimeSlot", weakestSlot);

        return result;
    }
}
