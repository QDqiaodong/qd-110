package com.habit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.habit.dto.ChallengeDTO;
import com.habit.dto.StreakResult;
import com.habit.entity.Challenge;
import com.habit.entity.Checkin;
import com.habit.entity.Habit;
import com.habit.mapper.ChallengeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChallengeService extends ServiceImpl<ChallengeMapper, Challenge> {
    
    @Autowired
    private HabitService habitService;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private StreakCalculatorService streakCalculatorService;
    
    public Challenge getActiveChallenge(Long habitId) {
        return this.getOne(new LambdaQueryWrapper<Challenge>()
                .eq(Challenge::getHabitId, habitId)
                .eq(Challenge::getStatus, "active")
                .orderByDesc(Challenge::getCreateTime)
                .last("LIMIT 1"));
    }
    
    public List<Challenge> getActiveChallenges() {
        return this.list(new LambdaQueryWrapper<Challenge>()
                .eq(Challenge::getStatus, "active")
                .orderByDesc(Challenge::getCreateTime));
    }
    
    public List<Challenge> getChallengeHistory() {
        return this.list(new LambdaQueryWrapper<Challenge>()
                .ne(Challenge::getStatus, "active")
                .orderByDesc(Challenge::getCreateTime));
    }
    
    @Transactional
    public Challenge startChallenge(ChallengeDTO dto) {
        Habit habit = habitService.getById(dto.getHabitId());
        if (habit == null || habit.getArchived()) {
            throw new RuntimeException("习惯不存在或已归档");
        }
        
        Challenge existing = getActiveChallenge(dto.getHabitId());
        if (existing != null) {
            throw new RuntimeException("该习惯已有进行中的挑战");
        }
        
        int totalDays = dto.getTotalDays() != null ? dto.getTotalDays() : 21;
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(totalDays - 1);
        
        Challenge challenge = new Challenge();
        challenge.setHabitId(habit.getId());
        challenge.setHabitName(habit.getName());
        challenge.setHabitColor(habit.getColor());
        challenge.setStartDate(startDate);
        challenge.setEndDate(endDate);
        challenge.setTotalDays(totalDays);
        challenge.setCompletedDays(0);
        challenge.setCurrentStreak(0);
        challenge.setMaxStreak(0);
        challenge.setStatus("active");
        challenge.setMilestone7(false);
        challenge.setMilestone14(false);
        challenge.setMilestone21(false);
        
        this.save(challenge);
        
        recalculateChallengeProgress(challenge);
        
        return challenge;
    }
    
    public Challenge getChallengeDetail(Long id) {
        Challenge challenge = this.getById(id);
        if (challenge != null) {
            recalculateChallengeProgress(challenge);
        }
        return challenge;
    }
    
    @Transactional
    public void recalculateChallengeProgress(Challenge challenge) {
        if (challenge == null || !"active".equals(challenge.getStatus())) {
            return;
        }

        LocalDate startDate = challenge.getStartDate();
        LocalDate today = LocalDate.now();

        List<Checkin> checkins = checkinService.list(
            new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getHabitId, challenge.getHabitId())
                .ge(Checkin::getCheckinDate, startDate)
                .le(Checkin::getCheckinDate, today)
                .eq(Checkin::getCompleted, true)
                .orderByAsc(Checkin::getCheckinDate)
        );

        int completedDays = checkins.size();

        StreakResult streak = streakCalculatorService.calculateForRange(
                challenge.getHabitId(), startDate, today.plusDays(1));

        int currentStreak = streak.getCurrentStreak() != null ? streak.getCurrentStreak() : 0;
        int maxStreak = streak.getMaxStreak() != null ? streak.getMaxStreak() : 0;

        challenge.setCompletedDays(completedDays);
        challenge.setCurrentStreak(currentStreak);
        challenge.setMaxStreak(Math.max(maxStreak, challenge.getMaxStreak() != null ? challenge.getMaxStreak() : 0));

        if (completedDays >= 7 && !challenge.getMilestone7()) {
            challenge.setMilestone7(true);
        }
        if (completedDays >= 14 && !challenge.getMilestone14()) {
            challenge.setMilestone14(true);
        }
        if (completedDays >= 21 && !challenge.getMilestone21()) {
            challenge.setMilestone21(true);
        }

        if (!today.isBefore(challenge.getEndDate()) && completedDays >= challenge.getTotalDays()) {
            challenge.setStatus("completed");
        }

        this.updateById(challenge);
    }
    
    public void recalculateAllChallenges() {
        List<Challenge> activeChallenges = getActiveChallenges();
        for (Challenge challenge : activeChallenges) {
            recalculateChallengeProgress(challenge);
        }
    }
    
    public int getRemainingDays(Challenge challenge) {
        if (challenge == null) return 0;
        LocalDate today = LocalDate.now();
        LocalDate endDate = challenge.getEndDate();
        if (today.isAfter(endDate)) return 0;
        return (int) ChronoUnit.DAYS.between(today, endDate) + 1;
    }
    
    public int getCurrentDay(Challenge challenge) {
        if (challenge == null) return 0;
        LocalDate today = LocalDate.now();
        LocalDate startDate = challenge.getStartDate();
        if (today.isBefore(startDate)) return 0;
        return (int) ChronoUnit.DAYS.between(startDate, today) + 1;
    }
    
    public boolean isInterrupted(Challenge challenge) {
        if (challenge == null || !"active".equals(challenge.getStatus())) return false;
        return challenge.getCurrentStreak() == 0 && challenge.getCompletedDays() > 0;
    }
    
    @Transactional
    public boolean giveUpChallenge(Long id) {
        Challenge challenge = this.getById(id);
        if (challenge == null) {
            return false;
        }
        challenge.setStatus("given_up");
        return this.updateById(challenge);
    }
}
