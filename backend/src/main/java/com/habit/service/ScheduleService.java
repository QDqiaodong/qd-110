package com.habit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.habit.dto.ScheduleDTO;
import com.habit.entity.Schedule;
import com.habit.mapper.ScheduleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ScheduleService extends ServiceImpl<ScheduleMapper, Schedule> {
    
    private static final String SCHEDULE_CACHE_KEY = "schedule:all";
    private static final String CURRENT_SCHEDULE_KEY = "schedule:current";
    private static final long CACHE_EXPIRE_HOURS = 2;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @SuppressWarnings("unchecked")
    public List<Schedule> getTemplates() {
        Object cached = redisTemplate.opsForValue().get(SCHEDULE_CACHE_KEY);
        if (cached != null) {
            return (List<Schedule>) cached;
        }
        
        List<Schedule> schedules = this.list(new LambdaQueryWrapper<Schedule>()
                .orderByAsc(Schedule::getIsCustom)
                .orderByDesc(Schedule::getCreateTime));
        
        if (schedules.isEmpty()) {
            schedules = initDefaultTemplates();
        }
        
        redisTemplate.opsForValue().set(SCHEDULE_CACHE_KEY, schedules, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return schedules;
    }
    
    @SuppressWarnings("unchecked")
    public Schedule getCurrentSchedule() {
        Object cached = redisTemplate.opsForValue().get(CURRENT_SCHEDULE_KEY);
        if (cached != null) {
            return (Schedule) cached;
        }
        
        Schedule current = this.getOne(new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getIsCurrent, true));
        
        if (current == null) {
            List<Schedule> all = getTemplates();
            if (!all.isEmpty()) {
                current = all.get(0);
                setCurrentSchedule(current.getId());
            }
        }
        
        if (current != null) {
            redisTemplate.opsForValue().set(CURRENT_SCHEDULE_KEY, current, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        }
        return current;
    }
    
    public Schedule setCurrentSchedule(Long id) {
        this.update(null, new LambdaUpdateWrapper<Schedule>()
                .set(Schedule::getIsCurrent, false));
        
        Schedule schedule = this.getById(id);
        if (schedule != null) {
            schedule.setIsCurrent(true);
            this.updateById(schedule);
        }
        
        clearCache();
        return schedule;
    }
    
    public Schedule createCustomSchedule(ScheduleDTO dto) {
        Schedule schedule = new Schedule();
        schedule.setName(dto.getName());
        schedule.setItems(dto.getItems());
        schedule.setIsCustom(true);
        schedule.setIsCurrent(false);
        this.save(schedule);
        
        clearCache();
        return schedule;
    }
    
    public boolean deleteSchedule(Long id) {
        Schedule schedule = this.getById(id);
        if (schedule == null || !schedule.getIsCustom()) {
            return false;
        }
        boolean result = this.removeById(id);
        if (result) {
            clearCache();
        }
        return result;
    }
    
    private List<Schedule> initDefaultTemplates() {
        String template1Items = "[{\"time\":\"06:00\",\"title\":\"起床洗漱\"},{\"time\":\"06:30\",\"title\":\"晨练运动\"},{\"time\":\"07:30\",\"title\":\"早餐\"},{\"time\":\"08:30\",\"title\":\"开始工作/学习\"},{\"time\":\"12:00\",\"title\":\"午餐休息\"},{\"time\":\"14:00\",\"title\":\"下午工作/学习\"},{\"time\":\"18:00\",\"title\":\"晚餐\"},{\"time\":\"19:00\",\"title\":\"自由活动\"},{\"time\":\"22:00\",\"title\":\"准备睡觉\"},{\"time\":\"22:30\",\"title\":\"入睡\"}]";
        String template2Items = "[{\"time\":\"07:00\",\"title\":\"起床早餐\"},{\"time\":\"08:00\",\"title\":\"早读\"},{\"time\":\"08:30\",\"title\":\"上午课程\"},{\"time\":\"12:00\",\"title\":\"午餐\"},{\"time\":\"13:00\",\"title\":\"午休\"},{\"time\":\"14:00\",\"title\":\"下午课程\"},{\"time\":\"18:00\",\"title\":\"晚餐\"},{\"time\":\"19:00\",\"title\":\"晚自习\"},{\"time\":\"21:30\",\"title\":\"自由活动\"},{\"time\":\"23:00\",\"title\":\"睡觉\"}]";
        String template3Items = "[{\"time\":\"06:30\",\"title\":\"起床\"},{\"time\":\"07:00\",\"title\":\"晨跑30分钟\"},{\"time\":\"08:00\",\"title\":\"早餐+蛋白质补充\"},{\"time\":\"09:00\",\"title\":\"工作/学习\"},{\"time\":\"12:00\",\"title\":\"午餐（高蛋白）\"},{\"time\":\"13:00\",\"title\":\"午休\"},{\"time\":\"14:00\",\"title\":\"工作/学习\"},{\"time\":\"17:30\",\"title\":\"健身房训练\"},{\"time\":\"19:30\",\"title\":\"晚餐\"},{\"time\":\"20:30\",\"title\":\"拉伸放松\"},{\"time\":\"22:30\",\"title\":\"睡觉\"}]";
        
        Schedule s1 = createTemplate("早起作息", template1Items);
        Schedule s2 = createTemplate("学生作息", template2Items);
        Schedule s3 = createTemplate("健身作息", template3Items);
        
        return List.of(s1, s2, s3);
    }
    
    private Schedule createTemplate(String name, String items) {
        Schedule schedule = new Schedule();
        schedule.setName(name);
        schedule.setItems(items);
        schedule.setIsCustom(false);
        schedule.setIsCurrent(false);
        this.save(schedule);
        return schedule;
    }
    
    private void clearCache() {
        redisTemplate.delete(SCHEDULE_CACHE_KEY);
        redisTemplate.delete(CURRENT_SCHEDULE_KEY);
    }
}
