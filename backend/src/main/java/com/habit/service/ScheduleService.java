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
    
    public Schedule copySchedule(Long id, ScheduleDTO dto) {
        Schedule source = this.getById(id);
        if (source == null) {
            return null;
        }
        
        Schedule newSchedule = new Schedule();
        newSchedule.setName(dto.getName() != null ? dto.getName() : source.getName() + " 副本");
        newSchedule.setWeekdayItems(dto.getWeekdayItems() != null ? dto.getWeekdayItems() : source.getWeekdayItems());
        newSchedule.setWeekendItems(dto.getWeekendItems() != null ? dto.getWeekendItems() : source.getWeekendItems());
        newSchedule.setItems(dto.getItems() != null ? dto.getItems() : source.getItems());
        newSchedule.setIsCustom(true);
        newSchedule.setIsCurrent(false);
        newSchedule.setVersion(source.getVersion() != null ? source.getVersion() + 1 : 2);
        newSchedule.setParentId(id);
        newSchedule.setTag(dto.getTag() != null ? dto.getTag() : source.getTag());
        newSchedule.setVersionNote(dto.getVersionNote());
        
        this.save(newSchedule);
        clearCache();
        return newSchedule;
    }
    
    public Schedule renameSchedule(Long id, ScheduleDTO dto) {
        Schedule schedule = this.getById(id);
        if (schedule == null) {
            return null;
        }
        
        if (dto.getName() != null) {
            schedule.setName(dto.getName());
        }
        if (dto.getTag() != null) {
            schedule.setTag(dto.getTag());
        }
        if (dto.getVersionNote() != null) {
            schedule.setVersionNote(dto.getVersionNote());
        }
        
        this.updateById(schedule);
        clearCache();
        return schedule;
    }
    
    public Schedule updateScheduleTag(Long id, String tag) {
        Schedule schedule = this.getById(id);
        if (schedule == null) {
            return null;
        }
        
        schedule.setTag(tag);
        this.updateById(schedule);
        clearCache();
        return schedule;
    }
    
    public List<Schedule> getTemplateVersions(Long id) {
        Schedule root = this.getById(id);
        if (root == null) {
            return List.of();
        }
        
        Long rootId = root.getParentId() != null ? root.getParentId() : id;
        List<Schedule> all = this.list();
        
        return all.stream()
                .filter(s -> s.getId().equals(rootId) || 
                        (s.getParentId() != null && (s.getParentId().equals(rootId) || 
                                all.stream().anyMatch(p -> p.getId().equals(rootId) && 
                                        p.getParentId() != null && p.getParentId().equals(s.getParentId())))))
                .sorted((a, b) -> {
                    if (a.getId().equals(rootId)) return -1;
                    if (b.getId().equals(rootId)) return 1;
                    if (a.getParentId() != null && a.getParentId().equals(rootId) && 
                            (b.getParentId() == null || !b.getParentId().equals(rootId))) return -1;
                    if (b.getParentId() != null && b.getParentId().equals(rootId) && 
                            (a.getParentId() == null || !a.getParentId().equals(rootId))) return 1;
                    return (b.getCreateTime() != null ? b.getCreateTime().compareTo(a.getCreateTime()) : 0);
                })
                .toList();
    }
    
    private List<Schedule> initDefaultTemplates() {
        String t1Weekday = "[{\"time\":\"06:00\",\"title\":\"起床洗漱\"},{\"time\":\"06:30\",\"title\":\"晨练运动\"},{\"time\":\"07:30\",\"title\":\"早餐\"},{\"time\":\"08:30\",\"title\":\"开始工作/学习\"},{\"time\":\"12:00\",\"title\":\"午餐休息\"},{\"time\":\"14:00\",\"title\":\"下午工作/学习\"},{\"time\":\"18:00\",\"title\":\"晚餐\"},{\"time\":\"19:00\",\"title\":\"自由活动\"},{\"time\":\"22:00\",\"title\":\"准备睡觉\"},{\"time\":\"22:30\",\"title\":\"入睡\"}]";
        String t1Weekend = "[{\"time\":\"08:00\",\"title\":\"自然醒起床\"},{\"time\":\"08:30\",\"title\":\"悠闲早餐\"},{\"time\":\"09:30\",\"title\":\"晨间运动/散步\"},{\"time\":\"11:00\",\"title\":\"处理家务\"},{\"time\":\"12:30\",\"title\":\"午餐\"},{\"time\":\"14:00\",\"title\":\"午休/阅读\"},{\"time\":\"16:00\",\"title\":\"兴趣爱好时间\"},{\"time\":\"18:30\",\"title\":\"晚餐\"},{\"time\":\"20:00\",\"title\":\"休闲娱乐\"},{\"time\":\"23:00\",\"title\":\"准备睡觉\"}]";
        String t2Weekday = "[{\"time\":\"07:00\",\"title\":\"起床早餐\"},{\"time\":\"08:00\",\"title\":\"早读\"},{\"time\":\"08:30\",\"title\":\"上午课程\"},{\"time\":\"12:00\",\"title\":\"午餐\"},{\"time\":\"13:00\",\"title\":\"午休\"},{\"time\":\"14:00\",\"title\":\"下午课程\"},{\"time\":\"18:00\",\"title\":\"晚餐\"},{\"time\":\"19:00\",\"title\":\"晚自习\"},{\"time\":\"21:30\",\"title\":\"自由活动\"},{\"time\":\"23:00\",\"title\":\"睡觉\"}]";
        String t2Weekend = "[{\"time\":\"09:00\",\"title\":\"起床\"},{\"time\":\"09:30\",\"title\":\"早餐\"},{\"time\":\"10:30\",\"title\":\"作业复习\"},{\"time\":\"12:30\",\"title\":\"午餐\"},{\"time\":\"14:00\",\"title\":\"午休\"},{\"time\":\"15:30\",\"title\":\"户外运动\"},{\"time\":\"18:00\",\"title\":\"晚餐\"},{\"time\":\"19:30\",\"title\":\"休闲娱乐\"},{\"time\":\"22:00\",\"title\":\"准备睡觉\"},{\"time\":\"22:30\",\"title\":\"入睡\"}]";
        String t3Weekday = "[{\"time\":\"06:30\",\"title\":\"起床\"},{\"time\":\"07:00\",\"title\":\"晨跑30分钟\"},{\"time\":\"08:00\",\"title\":\"早餐+蛋白质补充\"},{\"time\":\"09:00\",\"title\":\"工作/学习\"},{\"time\":\"12:00\",\"title\":\"午餐（高蛋白）\"},{\"time\":\"13:00\",\"title\":\"午休\"},{\"time\":\"14:00\",\"title\":\"工作/学习\"},{\"time\":\"17:30\",\"title\":\"健身房训练\"},{\"time\":\"19:30\",\"title\":\"晚餐\"},{\"time\":\"20:30\",\"title\":\"拉伸放松\"},{\"time\":\"22:30\",\"title\":\"睡觉\"}]";
        String t3Weekend = "[{\"time\":\"07:30\",\"title\":\"起床\"},{\"time\":\"08:00\",\"title\":\"轻量早餐\"},{\"time\":\"09:00\",\"title\":\"长时间户外训练\"},{\"time\":\"11:30\",\"title\":\"补充营养\"},{\"time\":\"13:00\",\"title\":\"午餐\"},{\"time\":\"14:30\",\"title\":\"午休\"},{\"time\":\"16:00\",\"title\":\"主动恢复/瑜伽\"},{\"time\":\"18:00\",\"title\":\"晚餐（欺骗餐）\"},{\"time\":\"20:00\",\"title\":\"休闲放松\"},{\"time\":\"22:30\",\"title\":\"睡觉\"}]";
        
        Schedule s1 = createTemplate("早起作息", t1Weekday, t1Weekend, "工作期");
        Schedule s2 = createTemplate("学生作息", t2Weekday, t2Weekend, "上学日");
        Schedule s3 = createTemplate("健身作息", t3Weekday, t3Weekend, "健身期");
        
        return List.of(s1, s2, s3);
    }
    
    private Schedule createTemplate(String name, String weekdayItems, String weekendItems, String tag) {
        Schedule schedule = new Schedule();
        schedule.setName(name);
        schedule.setWeekdayItems(weekdayItems);
        schedule.setWeekendItems(weekendItems);
        schedule.setItems(weekdayItems);
        schedule.setIsCustom(false);
        schedule.setIsCurrent(false);
        schedule.setVersion(1);
        schedule.setParentId(null);
        schedule.setTag(tag);
        schedule.setVersionNote("系统默认模板");
        this.save(schedule);
        return schedule;
    }
    
    private void clearCache() {
        redisTemplate.delete(SCHEDULE_CACHE_KEY);
        redisTemplate.delete(CURRENT_SCHEDULE_KEY);
    }
}
