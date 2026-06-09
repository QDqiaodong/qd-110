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

@Service
public class HabitService extends ServiceImpl<HabitMapper, Habit> {
    
    private static final String HABIT_CACHE_KEY = "habit:list";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    public List<Habit> getHabitList() {
        return this.list(new LambdaQueryWrapper<Habit>()
                .eq(Habit::getArchived, false)
                .orderByDesc(Habit::getStarred)
                .orderByAsc(Habit::getSortOrder)
                .orderByDesc(Habit::getCreateTime));
    }
    
    public List<Habit> getArchivedHabitList() {
        return this.list(new LambdaQueryWrapper<Habit>()
                .eq(Habit::getArchived, true)
                .orderByDesc(Habit::getArchiveTime)
                .orderByDesc(Habit::getCreateTime));
    }
    
    public Habit archiveHabit(Long id) {
        Habit habit = this.getById(id);
        if (habit == null) {
            return null;
        }
        habit.setArchived(true);
        habit.setArchiveTime(java.time.LocalDateTime.now());
        this.updateById(habit);
        clearCache();
        return habit;
    }
    
    public Habit unarchiveHabit(Long id) {
        Habit habit = this.getById(id);
        if (habit == null) {
            return null;
        }
        habit.setArchived(false);
        habit.setArchiveTime(null);
        this.updateById(habit);
        clearCache();
        return habit;
    }
    
    public Habit createHabit(HabitDTO dto) {
        Habit habit = new Habit();
        habit.setName(dto.getName());
        habit.setCategory(dto.getCategory() != null ? dto.getCategory() : "生活");
        habit.setTime(dto.getTime());
        habit.setRemind(dto.getRemind() != null ? dto.getRemind() : false);
        habit.setColor(dto.getColor() != null ? dto.getColor() : "#3b82f6");
        habit.setStarred(dto.getStarred() != null ? dto.getStarred() : false);
        
        if (dto.getStarred() != null && dto.getStarred()) {
            Integer maxSort = this.getMaxStarredSortOrder();
            habit.setSortOrder(maxSort != null ? maxSort + 1 : 1);
        } else {
            habit.setSortOrder(0);
        }
        
        this.save(habit);
        
        clearCache();
        return habit;
    }
    
    public Habit updateHabit(Long id, HabitDTO dto) {
        Habit habit = this.getById(id);
        if (habit == null) {
            return null;
        }
        
        boolean starChanged = false;
        boolean wasStarred = habit.getStarred() != null && habit.getStarred();
        
        if (dto.getName() != null) habit.setName(dto.getName());
        if (dto.getCategory() != null) habit.setCategory(dto.getCategory());
        if (dto.getTime() != null) habit.setTime(dto.getTime());
        if (dto.getRemind() != null) habit.setRemind(dto.getRemind());
        if (dto.getColor() != null) habit.setColor(dto.getColor());
        if (dto.getStarred() != null) {
            habit.setStarred(dto.getStarred());
            starChanged = true;
        }
        if (dto.getSortOrder() != null) habit.setSortOrder(dto.getSortOrder());
        
        if (starChanged) {
            if (dto.getStarred() && !wasStarred) {
                Integer maxSort = this.getMaxStarredSortOrder();
                habit.setSortOrder(maxSort != null ? maxSort + 1 : 1);
            } else if (!dto.getStarred() && wasStarred) {
                habit.setSortOrder(0);
            }
        }
        
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
    
    public Habit toggleStarred(Long id) {
        Habit habit = this.getById(id);
        if (habit == null) {
            return null;
        }
        
        boolean newStarred = !(habit.getStarred() != null && habit.getStarred());
        habit.setStarred(newStarred);
        
        if (newStarred) {
            Integer maxSort = this.getMaxStarredSortOrder();
            habit.setSortOrder(maxSort != null ? maxSort + 1 : 1);
        } else {
            habit.setSortOrder(0);
        }
        
        this.updateById(habit);
        clearCache();
        return habit;
    }
    
    public boolean updateStarredOrder(List<Long> habitIds) {
        if (habitIds == null || habitIds.isEmpty()) {
            return false;
        }
        
        for (int i = 0; i < habitIds.size(); i++) {
            Habit habit = this.getById(habitIds.get(i));
            if (habit != null) {
                habit.setSortOrder(i + 1);
                this.updateById(habit);
            }
        }
        
        clearCache();
        return true;
    }
    
    private Integer getMaxStarredSortOrder() {
        List<Habit> starredHabits = this.list(new LambdaQueryWrapper<Habit>()
                .eq(Habit::getStarred, true)
                .orderByDesc(Habit::getSortOrder)
                .last("LIMIT 1"));
        
        if (starredHabits != null && !starredHabits.isEmpty()) {
            return starredHabits.get(0).getSortOrder();
        }
        return 0;
    }
    
    private void clearCache() {
        redisTemplate.delete(HABIT_CACHE_KEY);
    }
}
