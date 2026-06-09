package com.habit.service;

import com.habit.entity.Checkin;
import com.habit.entity.Habit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class StatsService {
    
    private static final String STATS_CACHE_PREFIX = "stats:";
    private static final long CACHE_EXPIRE_MINUTES = 30;
    
    @Autowired
    private CheckinService checkinService;
    
    @Autowired
    private HabitService habitService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getWeekStats() {
        String cacheKey = STATS_CACHE_PREFIX + "week";
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return (List<Map<String, Object>>) cached;
            }
        } catch (SerializationException e) {
            redisTemplate.delete(cacheKey);
        }
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        
        List<Checkin> checkins = checkinService.getCheckinsByDateRange(startDate, endDate);
        List<Habit> habits = habitService.getHabitList();
        int totalHabits = habits.size();
        
        Set<Long> activeHabitIds = new HashSet<>();
        for (Habit habit : habits) {
            activeHabitIds.add(habit.getId());
        }
        
        Map<LocalDate, Integer> completedMap = new HashMap<>();
        for (Checkin checkin : checkins) {
            if (checkin.getCompleted() && activeHabitIds.contains(checkin.getHabitId())) {
                completedMap.merge(checkin.getCheckinDate(), 1, Integer::sum);
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = endDate.minusDays(i);
            int completed = completedMap.getOrDefault(date, 0);
            int rate = totalHabits > 0 ? Math.round((completed * 100.0f) / totalHabits) : 0;
            
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("label", date.getMonthValue() + "/" + date.getDayOfMonth());
            item.put("completed", completed);
            item.put("total", totalHabits);
            item.put("rate", rate);
            result.add(item);
        }
        
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMonthStats() {
        String cacheKey = STATS_CACHE_PREFIX + "month";
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return (List<Map<String, Object>>) cached;
            }
        } catch (SerializationException e) {
            redisTemplate.delete(cacheKey);
        }
        
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);
        
        List<Checkin> checkins = checkinService.getCheckinsByDateRange(startDate, endDate);
        List<Habit> habits = habitService.getHabitList();
        int totalHabits = habits.size();
        
        Set<Long> activeHabitIds = new HashSet<>();
        for (Habit habit : habits) {
            activeHabitIds.add(habit.getId());
        }
        
        Map<LocalDate, Integer> completedMap = new HashMap<>();
        for (Checkin checkin : checkins) {
            if (checkin.getCompleted() && activeHabitIds.contains(checkin.getHabitId())) {
                completedMap.merge(checkin.getCheckinDate(), 1, Integer::sum);
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 29; i >= 0; i -= 2) {
            LocalDate date = endDate.minusDays(i);
            int completed = completedMap.getOrDefault(date, 0);
            int rate = totalHabits > 0 ? Math.round((completed * 100.0f) / totalHabits) : 0;
            
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("label", date.getMonthValue() + "/" + date.getDayOfMonth());
            item.put("completed", completed);
            item.put("total", totalHabits);
            item.put("rate", rate);
            result.add(item);
        }
        
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return result;
    }
    
    public void clearStatsCache() {
        redisTemplate.delete(STATS_CACHE_PREFIX + "week");
        redisTemplate.delete(STATS_CACHE_PREFIX + "month");
    }
}
