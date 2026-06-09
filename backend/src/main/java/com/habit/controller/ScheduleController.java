package com.habit.controller;

import com.habit.dto.Result;
import com.habit.dto.ScheduleDTO;
import com.habit.entity.Schedule;
import com.habit.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/schedules")
public class ScheduleController {
    
    @Autowired
    private ScheduleService scheduleService;
    
    @GetMapping("/templates")
    public Result<List<Schedule>> getTemplates() {
        List<Schedule> schedules = scheduleService.getTemplates();
        return Result.success(schedules);
    }
    
    @GetMapping("/current")
    public Result<Schedule> getCurrentSchedule() {
        Schedule schedule = scheduleService.getCurrentSchedule();
        return Result.success(schedule);
    }
    
    @PutMapping("/current")
    public Result<Schedule> setCurrentSchedule(@RequestBody Map<String, Long> body) {
        Long id = body.get("id");
        Schedule schedule = scheduleService.setCurrentSchedule(id);
        return Result.success(schedule);
    }
    
    @PostMapping
    public Result<Schedule> createSchedule(@RequestBody ScheduleDTO dto) {
        Schedule schedule = scheduleService.createCustomSchedule(dto);
        return Result.success(schedule);
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> deleteSchedule(@PathVariable Long id) {
        boolean success = scheduleService.deleteSchedule(id);
        if (!success) {
            return Result.error("删除失败，仅可删除自定义模板");
        }
        return Result.success();
    }
}
