package com.habit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.habit.dto.HabitDTO;
import com.habit.entity.Habit;
import com.habit.mapper.HabitMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class HabitService extends ServiceImpl<HabitMapper, Habit> {
    
    private static final String HABIT_CACHE_KEY = "habit:list";
    private static final long CACHE_EXPIRE_HOURS = 1;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @SuppressWarnings("unchecked")
    public List<Habit> getHabitList() {
        Object cached = redisTemplate.opsForValue().get(HABIT_CACHE_KEY);
        if (cached != null) {
            return (List<Habit>) cached;
        }
        
        List<Habit> habits = this.list(new LambdaQueryWrapper<Habit>()
                .orderByDesc(Habit::getCreateTime));
        
        redisTemplate.opsForValue().set(HABIT_CACHE_KEY, habits, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return habits;
    }
    
    public Habit createHabit(HabitDTO dto) {
        Habit habit = new Habit();
        habit.setName(dto.getName());
        habit.setCategory(dto.getCategory() != null ? dto.getCategory() : "生活");
        habit.setTime(dto.getTime());
        habit.setRemind(dto.getRemind() != null ? dto.getRemind() : false);
        habit.setColor(dto.getColor() != null ? dto.getColor() : "#3b82f6");
        this.save(habit);
        
        clearCache();
        return habit;
    }
    
    public Habit updateHabit(Long id, HabitDTO dto) {
        Habit habit = this.getById(id);
        if (habit == null) {
            return null;
        }
        if (dto.getName() != null) habit.setName(dto.getName());
        if (dto.getCategory() != null) habit.setCategory(dto.getCategory());
        if (dto.getTime() != null) habit.setTime(dto.getTime());
        if (dto.getRemind() != null) habit.setRemind(dto.getRemind());
        if (dto.getColor() != null) habit.setColor(dto.getColor());
        this.updateById(habit);
        
        clearCache();
        return habit;
    }
    
    public boolean deleteHabit(Long id) {
        boolean result = this.removeById(id);
        if (result) {
            clearCache();
        }
        return result;
    }
    
    private void clearCache() {
        redisTemplate.delete(HABIT_CACHE_KEY);
    }
}
