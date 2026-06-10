package com.habit.controller;

import com.habit.dto.CheckinDTO;
import com.habit.dto.Result;
import com.habit.entity.Checkin;
import com.habit.entity.Challenge;
import com.habit.entity.HabitMilestone;
import com.habit.service.ChallengeService;
import com.habit.service.CheckinService;
import com.habit.service.HabitMilestoneService;
import com.habit.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/checkins")
public class CheckinController {
    
    @Autowired
    private CheckinService checkinService;
    
    @Autowired
    private StatsService statsService;
    
    @Autowired
    private ChallengeService challengeService;
    
    @Autowired
    private HabitMilestoneService habitMilestoneService;
    
    @GetMapping("/{date}")
    public Result<Map<Long, Boolean>> getCheckins(@PathVariable String date) {
        LocalDate checkinDate = LocalDate.parse(date);
        Map<Long, Boolean> checkins = checkinService.getCheckinsByDate(checkinDate);
        return Result.success(checkins);
    }
    
    @PostMapping
    public Result<Map<String, Object>> toggleCheckin(@RequestBody CheckinDTO dto) {
        LocalDate date = dto.getDate() != null ? dto.getDate() : LocalDate.now();
        Checkin checkin = checkinService.toggleCheckin(dto.getHabitId(), date);
        statsService.clearStatsCache();
        
        Challenge challenge = challengeService.getActiveChallenge(dto.getHabitId());
        Map<String, Object> milestoneInfo = null;
        if (challenge != null) {
            boolean wasMilestone7 = challenge.getMilestone7();
            boolean wasMilestone14 = challenge.getMilestone14();
            boolean wasMilestone21 = challenge.getMilestone21();
            
            challengeService.recalculateChallengeProgress(challenge);
            
            milestoneInfo = new HashMap<>();
            milestoneInfo.put("challengeId", challenge.getId());
            milestoneInfo.put("completedDays", challenge.getCompletedDays());
            milestoneInfo.put("currentStreak", challenge.getCurrentStreak());
            milestoneInfo.put("newMilestone7", challenge.getMilestone7() && !wasMilestone7);
            milestoneInfo.put("newMilestone14", challenge.getMilestone14() && !wasMilestone14);
            milestoneInfo.put("newMilestone21", challenge.getMilestone21() && !wasMilestone21);
            milestoneInfo.put("isCompleted", "completed".equals(challenge.getStatus()));
        }
        
        Map<String, Object> habitMilestoneInfo = null;
        if (checkin.getCompleted()) {
            HabitMilestone newMilestone = habitMilestoneService.checkAndCreateMilestone(dto.getHabitId(), date);
            if (newMilestone != null) {
                habitMilestoneInfo = habitMilestoneService.getMilestoneInfo(newMilestone);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("checkin", checkin);
        result.put("milestoneInfo", milestoneInfo);
        result.put("habitMilestoneInfo", habitMilestoneInfo);
        
        return Result.success(result);
    }
}
