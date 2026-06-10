package com.habit.controller;

import com.habit.dto.ChallengeDTO;
import com.habit.dto.Result;
import com.habit.entity.Challenge;
import com.habit.service.ChallengeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/challenges")
public class ChallengeController {
    
    @Autowired
    private ChallengeService challengeService;
    
    @GetMapping("/active")
    public Result<List<Challenge>> getActiveChallenges() {
        challengeService.recalculateAllChallenges();
        List<Challenge> challenges = challengeService.getActiveChallenges();
        return Result.success(challenges);
    }
    
    @GetMapping("/history")
    public Result<List<Challenge>> getChallengeHistory() {
        List<Challenge> challenges = challengeService.getChallengeHistory();
        return Result.success(challenges);
    }
    
    @GetMapping("/habit/{habitId}")
    public Result<Challenge> getChallengeByHabit(@PathVariable Long habitId) {
        Challenge challenge = challengeService.getActiveChallenge(habitId);
        if (challenge != null) {
            challengeService.recalculateChallengeProgress(challenge);
        }
        return Result.success(challenge);
    }
    
    @GetMapping("/{id}")
    public Result<Challenge> getChallengeDetail(@PathVariable Long id) {
        Challenge challenge = challengeService.getChallengeDetail(id);
        if (challenge == null) {
            return Result.error("挑战不存在");
        }
        return Result.success(challenge);
    }
    
    @PostMapping
    public Result<Challenge> startChallenge(@RequestBody ChallengeDTO dto) {
        try {
            Challenge challenge = challengeService.startChallenge(dto);
            return Result.success(challenge);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }
    
    @PostMapping("/{id}/refresh")
    public Result<Challenge> refreshChallenge(@PathVariable Long id) {
        Challenge challenge = challengeService.getById(id);
        if (challenge == null) {
            return Result.error("挑战不存在");
        }
        challengeService.recalculateChallengeProgress(challenge);
        return Result.success(challenge);
    }
    
    @PostMapping("/{id}/give-up")
    public Result<Void> giveUpChallenge(@PathVariable Long id) {
        boolean success = challengeService.giveUpChallenge(id);
        if (!success) {
            return Result.error("放弃失败");
        }
        return Result.success();
    }
    
    @GetMapping("/{id}/stats")
    public Result<Map<String, Object>> getChallengeStats(@PathVariable Long id) {
        Challenge challenge = challengeService.getById(id);
        if (challenge == null) {
            return Result.error("挑战不存在");
        }
        
        challengeService.recalculateChallengeProgress(challenge);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("challenge", challenge);
        stats.put("remainingDays", challengeService.getRemainingDays(challenge));
        stats.put("currentDay", challengeService.getCurrentDay(challenge));
        stats.put("isInterrupted", challengeService.isInterrupted(challenge));
        
        return Result.success(stats);
    }
}
