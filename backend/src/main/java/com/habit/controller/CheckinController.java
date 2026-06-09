package com.habit.controller;

import com.habit.dto.CheckinDTO;
import com.habit.dto.Result;
import com.habit.entity.Checkin;
import com.habit.service.CheckinService;
import com.habit.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/checkins")
public class CheckinController {
    
    @Autowired
    private CheckinService checkinService;
    
    @Autowired
    private StatsService statsService;
    
    @GetMapping("/{date}")
    public Result<Map<Long, Boolean>> getCheckins(@PathVariable String date) {
        LocalDate checkinDate = LocalDate.parse(date);
        Map<Long, Boolean> checkins = checkinService.getCheckinsByDate(checkinDate);
        return Result.success(checkins);
    }
    
    @PostMapping
    public Result<Checkin> toggleCheckin(@RequestBody CheckinDTO dto) {
        LocalDate date = dto.getDate() != null ? dto.getDate() : LocalDate.now();
        Checkin checkin = checkinService.toggleCheckin(dto.getHabitId(), date);
        statsService.clearStatsCache();
        return Result.success(checkin);
    }
}
