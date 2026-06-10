package com.habit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.habit.entity.Checkin;
import com.habit.entity.Habit;
import com.habit.entity.HabitMilestone;
import com.habit.mapper.HabitMilestoneMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
public class HabitMilestoneService extends ServiceImpl<HabitMilestoneMapper, HabitMilestone> {
    
    public static final int[] MILESTONE_TYPES = {7, 30, 100, 365};
    
    public static final Map<Integer, String> MILESTONE_LABELS = new HashMap<>();
    public static final Map<Integer, String> MILESTONE_ICONS = new HashMap<>();
    public static final Map<Integer, String> MILESTONE_MESSAGES = new HashMap<>();
    
    static {
        MILESTONE_LABELS.put(7, "初露锋芒");
        MILESTONE_LABELS.put(30, "月度坚持");
        MILESTONE_LABELS.put(100, "百日达人");
        MILESTONE_LABELS.put(365, "年度传奇");
        
        MILESTONE_ICONS.put(7, "🌱");
        MILESTONE_ICONS.put(30, "🌿");
        MILESTONE_ICONS.put(100, "🌳");
        MILESTONE_ICONS.put(365, "🏆");
        
        MILESTONE_MESSAGES.put(7, "太棒了！你已经累计打卡7次，好习惯正在萌芽，继续加油！");
        MILESTONE_MESSAGES.put(30, "一个月了！你的坚持令人钦佩，这个习惯已经融入你的日常生活。");
        MILESTONE_MESSAGES.put(100, "百日达成！你已经把这个习惯坚持了100次，这是真正的自律！");
        MILESTONE_MESSAGES.put(365, "一年传奇！你用365次打卡证明了坚持的力量，这个习惯已经成为你的一部分！");
    }
    
    @Autowired
    private CheckinService checkinService;
    
    @Autowired
    private HabitService habitService;
    
    public List<HabitMilestone> getMilestonesByHabit(Long habitId) {
        return this.list(new LambdaQueryWrapper<HabitMilestone>()
                .eq(HabitMilestone::getHabitId, habitId)
                .orderByAsc(HabitMilestone::getMilestoneType));
    }
    
    public List<HabitMilestone> getAllMilestones() {
        return this.list(new LambdaQueryWrapper<HabitMilestone>()
                .orderByDesc(HabitMilestone::getAchieveDate));
    }
    
    @Transactional
    public HabitMilestone checkAndCreateMilestone(Long habitId, LocalDate checkinDate) {
        int totalCheckins = countTotalCheckins(habitId);
        
        Set<Integer> achievedTypes = getAchievedMilestoneTypes(habitId);
        
        for (int type : MILESTONE_TYPES) {
            if (totalCheckins >= type && !achievedTypes.contains(type)) {
                return createMilestone(habitId, type, totalCheckins, checkinDate);
            }
        }
        
        return null;
    }
    
    private int countTotalCheckins(Long habitId) {
        long count = checkinService.count(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getHabitId, habitId)
                .eq(Checkin::getCompleted, true));
        return (int) count;
    }
    
    private Set<Integer> getAchievedMilestoneTypes(Long habitId) {
        List<HabitMilestone> milestones = this.list(new LambdaQueryWrapper<HabitMilestone>()
                .eq(HabitMilestone::getHabitId, habitId));
        Set<Integer> types = new HashSet<>();
        for (HabitMilestone m : milestones) {
            types.add(m.getMilestoneType());
        }
        return types;
    }
    
    private HabitMilestone createMilestone(Long habitId, int milestoneType, int totalCheckins, LocalDate achieveDate) {
        Habit habit = habitService.getById(habitId);
        if (habit == null) {
            return null;
        }
        
        HabitMilestone milestone = new HabitMilestone();
        milestone.setHabitId(habitId);
        milestone.setHabitName(habit.getName());
        milestone.setHabitColor(habit.getColor());
        milestone.setMilestoneType(milestoneType);
        milestone.setMilestoneLabel(MILESTONE_LABELS.get(milestoneType));
        milestone.setMilestoneIcon(MILESTONE_ICONS.get(milestoneType));
        milestone.setAchieveDate(achieveDate);
        milestone.setTotalCheckins(totalCheckins);
        
        this.save(milestone);
        return milestone;
    }
    
    public Map<String, Object> getMilestoneInfo(HabitMilestone milestone) {
        if (milestone == null) {
            return null;
        }
        Map<String, Object> info = new HashMap<>();
        info.put("milestoneId", milestone.getId());
        info.put("habitId", milestone.getHabitId());
        info.put("habitName", milestone.getHabitName());
        info.put("habitColor", milestone.getHabitColor());
        info.put("milestoneType", milestone.getMilestoneType());
        info.put("milestoneLabel", milestone.getMilestoneLabel());
        info.put("milestoneIcon", milestone.getMilestoneIcon());
        info.put("achieveDate", milestone.getAchieveDate().toString());
        info.put("totalCheckins", milestone.getTotalCheckins());
        info.put("message", MILESTONE_MESSAGES.get(milestone.getMilestoneType()));
        return info;
    }
    
    public List<Map<String, Object>> getAllMilestoneDefinitions() {
        List<Map<String, Object>> definitions = new ArrayList<>();
        for (int type : MILESTONE_TYPES) {
            Map<String, Object> def = new HashMap<>();
            def.put("type", type);
            def.put("label", MILESTONE_LABELS.get(type));
            def.put("icon", MILESTONE_ICONS.get(type));
            def.put("message", MILESTONE_MESSAGES.get(type));
            definitions.add(def);
        }
        return definitions;
    }
}
