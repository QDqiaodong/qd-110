package com.habit.controller;

import com.habit.dto.Result;
import com.habit.dto.StreakResult;
import com.habit.entity.Checkin;
import com.habit.entity.Habit;
import com.habit.entity.HabitMilestone;
import com.habit.service.CheckinService;
import com.habit.service.HabitMilestoneService;
import com.habit.service.HabitService;
import com.habit.service.StreakCalculatorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/habit-milestones")
public class HabitMilestoneController {
    
    @Autowired
    private HabitMilestoneService habitMilestoneService;

    @Autowired
    private HabitService habitService;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private StreakCalculatorService streakCalculatorService;
    
    @GetMapping("/habit/{habitId}")
    public Result<Map<String, Object>> getHabitMilestones(@PathVariable Long habitId) {
        Habit habit = habitService.getById(habitId);
        if (habit == null) {
            return Result.error("习惯不存在");
        }
        
        List<HabitMilestone> achievedMilestones = habitMilestoneService.getMilestonesByHabit(habitId);
        Set<Integer> achievedTypes = achievedMilestones.stream()
                .map(HabitMilestone::getMilestoneType)
                .collect(Collectors.toSet());
        
        List<Map<String, Object>> milestoneDefinitions = habitMilestoneService.getAllMilestoneDefinitions();
        List<Map<String, Object>> milestonesWithStatus = new ArrayList<>();
        for (Map<String, Object> def : milestoneDefinitions) {
            int type = (int) def.get("type");
            Map<String, Object> item = new HashMap<>(def);
            item.put("achieved", achievedTypes.contains(type));
            HabitMilestone achieved = achievedMilestones.stream()
                    .filter(m -> m.getMilestoneType() == type)
                    .findFirst()
                    .orElse(null);
            if (achieved != null) {
                item.put("achieveDate", achieved.getAchieveDate().toString());
                item.put("totalCheckins", achieved.getTotalCheckins());
                item.put("milestoneId", achieved.getId());
            }
            milestonesWithStatus.add(item);
        }
        
        Map<String, Object> habitStats = calculateHabitStats(habitId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("habit", habit);
        result.put("milestones", milestonesWithStatus);
        result.put("stats", habitStats);
        result.put("recentCheckins", getRecentCheckins(habitId, 30));
        
        return Result.success(result);
    }
    
    @GetMapping("/all")
    public Result<List<HabitMilestone>> getAllMilestones() {
        List<HabitMilestone> milestones = habitMilestoneService.getAllMilestones();
        return Result.success(milestones);
    }
    
    @GetMapping("/definitions")
    public Result<List<Map<String, Object>>> getMilestoneDefinitions() {
        return Result.success(habitMilestoneService.getAllMilestoneDefinitions());
    }
    
    private Map<String, Object> calculateHabitStats(Long habitId) {
        StreakResult streak = streakCalculatorService.calculateForHabit(habitId);

        Map<String, Object> stats = new HashMap<>();
        int totalCompleted = streak.getTotalCompletedDays() != null ? streak.getTotalCompletedDays() : 0;
        stats.put("totalCompleted", totalCompleted);
        stats.put("currentStreak", streak.getCurrentStreak() != null ? streak.getCurrentStreak() : 0);
        stats.put("maxStreak", streak.getMaxStreak() != null ? streak.getMaxStreak() : 0);
        stats.put("firstCheckinDate", streak.getFirstCheckinDate() != null ? streak.getFirstCheckinDate().toString() : null);
        stats.put("lastCheckinDate", streak.getLastCheckinDate() != null ? streak.getLastCheckinDate().toString() : null);

        long daysSinceFirst = 0;
        int completionRate = 0;
        if (streak.getFirstCheckinDate() != null) {
            LocalDate today = LocalDate.now();
            daysSinceFirst = ChronoUnit.DAYS.between(streak.getFirstCheckinDate(), today) + 1;
            completionRate = daysSinceFirst > 0 ? Math.round((totalCompleted * 100.0f) / daysSinceFirst) : 0;
        }
        stats.put("daysSinceFirst", daysSinceFirst);
        stats.put("completionRate", completionRate);

        stats.put("segments", streak.getSegments());
        stats.put("breakPoints", streak.getBreakPoints());
        stats.put("totalSegments", streak.getTotalSegments());
        stats.put("totalBreakPoints", streak.getTotalBreakPoints());

        return stats;
    }
    
    private List<Map<String, Object>> getRecentCheckins(Long habitId, int days) {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        
        Map<LocalDate, Boolean> checkinMap = new HashMap<>();
        List<Checkin> checkins = checkinService.list(
                new LambdaQueryWrapper<Checkin>()
                        .eq(Checkin::getHabitId, habitId)
                        .ge(Checkin::getCheckinDate, today.minusDays(days - 1))
                        .le(Checkin::getCheckinDate, today)
        );
        for (Checkin c : checkins) {
            checkinMap.put(c.getCheckinDate(), c.getCompleted());
        }
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("label", date.getMonthValue() + "/" + date.getDayOfMonth());
            item.put("weekday", getWeekdayName(date));
            item.put("completed", checkinMap.getOrDefault(date, false));
            result.add(item);
        }
        
        return result;
    }
    
    private String getWeekdayName(LocalDate date) {
        String[] weekdays = {"日", "一", "二", "三", "四", "五", "六"};
        return "周" + weekdays[date.getDayOfWeek().getValue() % 7];
    }
}
