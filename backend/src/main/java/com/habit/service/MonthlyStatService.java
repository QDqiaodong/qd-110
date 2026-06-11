package com.habit.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.habit.entity.Checkin;
import com.habit.entity.Habit;
import com.habit.entity.MonthlyStat;
import com.habit.event.CheckinChangedEvent;
import com.habit.mapper.MonthlyStatMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MonthlyStatService extends ServiceImpl<MonthlyStatMapper, MonthlyStat> {
    
    private static final Logger log = LoggerFactory.getLogger(MonthlyStatService.class);
    
    @Autowired
    private CheckinService checkinService;
    
    @Autowired
    private HabitService habitService;
    
    @Autowired
    private StatsService statsService;
    
    @Autowired
    private MonthlyStatMapper monthlyStatMapper;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    public MonthlyStat getMonthlyStat(Integer year, Integer month) {
        MonthlyStat stat = monthlyStatMapper.selectByYearMonth(year, month);
        if (stat == null) {
            stat = calculateMonthlyStat(year, month);
        }
        return stat;
    }
    
    public List<Map<String, Object>> getMonthStatsWithTrend() {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth startMonth = currentMonth.minusMonths(5);
        
        List<MonthlyStat> monthlyStats = monthlyStatMapper.selectByDateRange(
            startMonth.atDay(1), 
            currentMonth.atEndOfMonth()
        );
        
        Map<YearMonth, MonthlyStat> statMap = monthlyStats.stream()
            .collect(Collectors.toMap(
                s -> YearMonth.of(s.getStatYear(), s.getStatMonth()),
                s -> s
            ));
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = currentMonth.minusMonths(i);
            MonthlyStat stat = statMap.get(ym);
            
            if (stat == null) {
                stat = calculateMonthlyStat(ym.getYear(), ym.getMonthValue());
            }
            
            Map<String, Object> item = new HashMap<>();
            item.put("year", ym.getYear());
            item.put("month", ym.getMonthValue());
            item.put("label", ym.getMonthValue() + "月");
            item.put("completionRate", stat.getCompletionRate());
            item.put("totalHabits", stat.getTotalHabits());
            item.put("avgCompletedPerDay", stat.getAvgCompletedPerDay());
            
            try {
                if (stat.getTrendData() != null && !stat.getTrendData().isEmpty()) {
                    List<Map<String, Object>> trend = objectMapper.readValue(
                        stat.getTrendData(), 
                        new TypeReference<List<Map<String, Object>>>() {}
                    );
                    item.put("trendData", trend);
                }
            } catch (Exception e) {
                log.warn("解析趋势数据失败", e);
            }
            
            result.add(item);
        }
        
        return result;
    }
    
    @Transactional
    public MonthlyStat calculateMonthlyStat(Integer year, Integer month) {
        log.info("开始计算 {}-{} 月统计数据", year, month);
        
        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate = ym.atEndOfMonth();
        LocalDate today = LocalDate.now();
        
        if (endDate.isAfter(today)) {
            endDate = today;
        }
        
        List<Habit> habits = habitService.getHabitList();
        int totalHabits = habits.size();
        
        Set<Long> activeHabitIds = habits.stream()
            .map(Habit::getId)
            .collect(Collectors.toSet());
        
        List<Checkin> checkins = checkinService.getCheckinsByDateRange(startDate, endDate);
        
        Map<LocalDate, Integer> completedMap = new HashMap<>();
        Set<LocalDate> checkinDays = new HashSet<>();
        
        for (Checkin checkin : checkins) {
            if (checkin.getCompleted() && activeHabitIds.contains(checkin.getHabitId())) {
                completedMap.merge(checkin.getCheckinDate(), 1, Integer::sum);
                checkinDays.add(checkin.getCheckinDate());
            }
        }
        
        int daysInRange = endDate.getDayOfMonth();
        int totalCheckinDays = checkinDays.size();
        
        int totalCompleted = completedMap.values().stream().mapToInt(Integer::intValue).sum();
        int avgCompletedPerDay = daysInRange > 0 ? Math.round((float) totalCompleted / daysInRange) : 0;
        
        int completionRate = 0;
        if (totalHabits > 0 && daysInRange > 0) {
            double totalPossible = (double) totalHabits * daysInRange;
            completionRate = (int) Math.round((totalCompleted / totalPossible) * 100);
        }
        
        List<Map<String, Object>> trendData = buildTrendData(startDate, endDate, completedMap, totalHabits);
        
        MonthlyStat existing = monthlyStatMapper.selectByYearMonth(year, month);
        MonthlyStat stat;
        
        if (existing != null) {
            stat = existing;
        } else {
            stat = new MonthlyStat();
            stat.setStatYear(year);
            stat.setStatMonth(month);
        }
        
        stat.setTotalHabits(totalHabits);
        stat.setTotalCheckinDays(totalCheckinDays);
        stat.setAvgCompletedPerDay(avgCompletedPerDay);
        stat.setCompletionRate(completionRate);
        
        try {
            stat.setTrendData(objectMapper.writeValueAsString(trendData));
        } catch (Exception e) {
            log.warn("序列化趋势数据失败", e);
        }
        
        if (existing != null) {
            this.updateById(stat);
        } else {
            this.save(stat);
        }
        
        log.info("{}-{} 月统计计算完成，完成率: {}%", year, month, completionRate);
        
        return stat;
    }
    
    private List<Map<String, Object>> buildTrendData(LocalDate startDate, LocalDate endDate,
                                                      Map<LocalDate, Integer> completedMap,
                                                      int totalHabits) {
        List<Map<String, Object>> trend = new ArrayList<>();
        int totalDays = endDate.getDayOfMonth();
        int step = totalDays > 15 ? 2 : 1;
        
        for (int i = 0; i < totalDays; i += step) {
            LocalDate date = startDate.plusDays(i);
            if (date.isAfter(endDate)) {
                break;
            }
            
            int completed = completedMap.getOrDefault(date, 0);
            int rate = totalHabits > 0 ? Math.round((completed * 100.0f) / totalHabits) : 0;
            
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("day", date.getDayOfMonth());
            item.put("completed", completed);
            item.put("rate", rate);
            trend.add(item);
        }
        
        return trend;
    }
    
    @Async("monthlyStatTaskExecutor")
    public void asyncRecalculateMonth(Integer year, Integer month) {
        try {
            log.info("异步补算 {}-{} 月统计开始", year, month);
            calculateMonthlyStat(year, month);
            statsService.clearStatsCache();
            log.info("异步补算 {}-{} 月统计完成", year, month);
        } catch (Exception e) {
            log.error("异步补算 {}-{} 月统计失败", year, month, e);
        }
    }
    
    @Async("monthlyStatTaskExecutor")
    public void asyncRecalculateForDate(LocalDate date) {
        if (date == null) {
            return;
        }
        int year = date.getYear();
        int month = date.getMonthValue();
        asyncRecalculateMonth(year, month);
    }
    
    @Async("monthlyStatTaskExecutor")
    public void asyncRecalculateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return;
        }
        
        YearMonth startYm = YearMonth.from(startDate);
        YearMonth endYm = YearMonth.from(endDate);
        
        YearMonth current = startYm;
        while (!current.isAfter(endYm)) {
            asyncRecalculateMonth(current.getYear(), current.getMonthValue());
            current = current.plusMonths(1);
        }
    }
    
    public List<MonthlyStat> getRecentMonthlyStats(int months) {
        LocalDate today = LocalDate.now();
        YearMonth endMonth = YearMonth.from(today);
        YearMonth startMonth = endMonth.minusMonths(months - 1);
        
        return monthlyStatMapper.selectByDateRange(
            startMonth.atDay(1),
            endMonth.atEndOfMonth()
        );
    }
    
    public boolean hasMonthlyStat(Integer year, Integer month) {
        return monthlyStatMapper.selectByYearMonth(year, month) != null;
    }
    
    @EventListener
    @Async("monthlyStatTaskExecutor")
    public void onCheckinChanged(CheckinChangedEvent event) {
        try {
            LocalDate date = event.getDate();
            CheckinChangedEvent.ChangeType type = event.getChangeType();
            
            log.debug("收到打卡变更事件: type={}, date={}", type, date);
            
            if (type == CheckinChangedEvent.ChangeType.CHECKIN_TOGGLE && date != null) {
                asyncRecalculateForDate(date);
            } else if (type == CheckinChangedEvent.ChangeType.HABIT_CHANGED
                    || type == CheckinChangedEvent.ChangeType.HABIT_DELETED) {
                LocalDate today = LocalDate.now();
                asyncRecalculateMonth(today.getYear(), today.getMonthValue());
                
                if (today.getDayOfMonth() <= 7) {
                    LocalDate lastMonth = today.minusMonths(1);
                    asyncRecalculateMonth(lastMonth.getYear(), lastMonth.getMonthValue());
                }
            }
        } catch (Exception e) {
            log.error("处理打卡变更事件失败", e);
        }
    }
}
