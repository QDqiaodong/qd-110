package com.habit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.habit.entity.Checkin;
import com.habit.mapper.CheckinMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CheckinService extends ServiceImpl<CheckinMapper, Checkin> {
    
    private static final String CHECKIN_CACHE_PREFIX = "checkin:";
    private static final long CACHE_EXPIRE_HOURS = 24;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private CheckinMapper checkinMapper;
    
    @SuppressWarnings("unchecked")
    public Map<Long, Boolean> getCheckinsByDate(LocalDate date) {
        String cacheKey = CHECKIN_CACHE_PREFIX + date;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (Map<Long, Boolean>) cached;
        }
        
        List<Checkin> checkins = this.list(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getCheckinDate, date));
        
        Map<Long, Boolean> result = new HashMap<>();
        for (Checkin checkin : checkins) {
            result.put(checkin.getHabitId(), checkin.getCompleted());
        }
        
        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return result;
    }
    
    @Transactional
    public synchronized Checkin toggleCheckin(Long habitId, LocalDate date) {
        Checkin existing = this.getOne(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getHabitId, habitId)
                .eq(Checkin::getCheckinDate, date));
        
        if (existing != null) {
            existing.setCompleted(!existing.getCompleted());
            this.updateById(existing);
        } else {
            existing = new Checkin();
            existing.setHabitId(habitId);
            existing.setCheckinDate(date);
            existing.setCompleted(true);
            this.save(existing);
        }
        
        clearDateCache(date);
        return existing;
    }
    
    public List<Checkin> getCheckinsByDateRange(LocalDate startDate, LocalDate endDate) {
        return checkinMapper.selectByDateRange(startDate, endDate);
    }
    
    private void clearDateCache(LocalDate date) {
        redisTemplate.delete(CHECKIN_CACHE_PREFIX + date);
    }
}
