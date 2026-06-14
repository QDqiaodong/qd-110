package com.habit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.habit.dto.QuietHourRuleDTO;
import com.habit.entity.Habit;
import com.habit.entity.QuietHourRule;
import com.habit.mapper.QuietHourRuleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class QuietHourRuleService extends ServiceImpl<QuietHourRuleMapper, QuietHourRule> {

    private static final Logger log = LoggerFactory.getLogger(QuietHourRuleService.class);
    private static final String QUIET_HOUR_CACHE_KEY = "quiet:hour:all";
    private static final String QUIET_HOUR_ENABLED_CACHE_KEY = "quiet:hour:enabled";
    private static final long CACHE_EXPIRE_HOURS = 2;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @SuppressWarnings("unchecked")
    public List<QuietHourRule> getAllRules() {
        Object cached = redisTemplate.opsForValue().get(QUIET_HOUR_CACHE_KEY);
        if (cached != null) {
            return (List<QuietHourRule>) cached;
        }

        List<QuietHourRule> rules = this.list(new LambdaQueryWrapper<QuietHourRule>()
                .orderByAsc(QuietHourRule::getSortOrder)
                .orderByDesc(QuietHourRule::getCreateTime));

        if (rules.isEmpty()) {
            rules = initDefaultRules();
        }

        redisTemplate.opsForValue().set(QUIET_HOUR_CACHE_KEY, rules, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return rules;
    }

    @SuppressWarnings("unchecked")
    public List<QuietHourRule> getEnabledRules() {
        Object cached = redisTemplate.opsForValue().get(QUIET_HOUR_ENABLED_CACHE_KEY);
        if (cached != null) {
            return (List<QuietHourRule>) cached;
        }

        List<QuietHourRule> rules = this.list(new LambdaQueryWrapper<QuietHourRule>()
                .eq(QuietHourRule::getEnabled, true)
                .orderByAsc(QuietHourRule::getSortOrder));

        redisTemplate.opsForValue().set(QUIET_HOUR_ENABLED_CACHE_KEY, rules, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return rules;
    }

    public boolean isTimeInQuietHour(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return false;
        }
        LocalTime time;
        try {
            time = parseTime(timeStr);
        } catch (Exception e) {
            return false;
        }
        return isTimeInQuietHour(time);
    }

    public boolean isTimeInQuietHour(LocalTime time) {
        if (time == null) {
            return false;
        }
        List<QuietHourRule> rules = getEnabledRules();
        for (QuietHourRule rule : rules) {
            if (isTimeInRange(time, rule.getStartTime(), rule.getEndTime())) {
                return true;
            }
        }
        return false;
    }

    public QuietHourRule getMatchingQuietHour(String timeStr) {
        if (timeStr == null || timeStr.isEmpty()) {
            return null;
        }
        LocalTime time;
        try {
            time = parseTime(timeStr);
        } catch (Exception e) {
            return null;
        }
        List<QuietHourRule> rules = getEnabledRules();
        for (QuietHourRule rule : rules) {
            if (isTimeInRange(time, rule.getStartTime(), rule.getEndTime())) {
                return rule;
            }
        }
        return null;
    }

    public List<Habit> filterHabitsByQuietHours(List<Habit> habits) {
        List<QuietHourRule> enabledRules = getEnabledRules();
        if (enabledRules.isEmpty()) {
            return habits;
        }

        return habits.stream()
                .filter(habit -> {
                    if (habit.getRemind() == null || !habit.getRemind()) {
                        return true;
                    }
                    String habitTime = habit.getTime();
                    if (habitTime == null || habitTime.isEmpty()) {
                        return true;
                    }
                    try {
                        LocalTime time = parseTime(habitTime);
                        for (QuietHourRule rule : enabledRules) {
                            if (isTimeInRange(time, rule.getStartTime(), rule.getEndTime())) {
                                log.debug("习惯[{}]时间{}落在安静时段[{}]内，过滤该提醒",
                                        habit.getName(), habitTime, rule.getName());
                                return false;
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析习惯时间失败: {}", habitTime, e);
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    public Map<String, Object> getReminderSchedule(List<Habit> habits) {
        Map<String, Object> result = new HashMap<>();
        List<QuietHourRule> enabledRules = getEnabledRules();
        result.put("quietHours", enabledRules);

        List<Map<String, Object>> effectiveReminders = new ArrayList<>();
        List<Map<String, Object>> skippedReminders = new ArrayList<>();

        for (Habit habit : habits) {
            if (habit.getRemind() == null || !habit.getRemind()) {
                continue;
            }
            String habitTime = habit.getTime();
            if (habitTime == null || habitTime.isEmpty()) {
                continue;
            }

            Map<String, Object> reminder = new HashMap<>();
            reminder.put("habitId", habit.getId());
            reminder.put("habitName", habit.getName());
            reminder.put("time", habitTime);
            reminder.put("color", habit.getColor());

            try {
                LocalTime time = parseTime(habitTime);
                QuietHourRule matchedRule = null;
                for (QuietHourRule rule : enabledRules) {
                    if (isTimeInRange(time, rule.getStartTime(), rule.getEndTime())) {
                        matchedRule = rule;
                        break;
                    }
                }

                if (matchedRule != null) {
                    reminder.put("skipped", true);
                    reminder.put("quietHourName", matchedRule.getName());
                    reminder.put("suggestedTime", suggestAlternativeTime(habitTime, matchedRule));
                    skippedReminders.add(reminder);
                } else {
                    reminder.put("skipped", false);
                    effectiveReminders.add(reminder);
                }
            } catch (Exception e) {
                reminder.put("skipped", false);
                effectiveReminders.add(reminder);
            }
        }

        effectiveReminders.sort(Comparator.comparing(r -> (String) r.get("time")));

        result.put("effectiveReminders", effectiveReminders);
        result.put("skippedReminders", skippedReminders);
        result.put("totalReminders", habits.stream().filter(h -> Boolean.TRUE.equals(h.getRemind()) && h.getTime() != null).count());
        result.put("effectiveCount", effectiveReminders.size());
        result.put("skippedCount", skippedReminders.size());

        return result;
    }

    public String suggestAlternativeTime(String originalTime, QuietHourRule rule) {
        try {
            LocalTime original = parseTime(originalTime);
            LocalTime start = parseTime(rule.getStartTime());
            LocalTime end = parseTime(rule.getEndTime());

            if (isCrossDayRange(rule.getStartTime(), rule.getEndTime())) {
                return rule.getEndTime();
            }

            long durationToStart = Math.abs(java.time.Duration.between(original, start).toMinutes());
            long durationToEnd = Math.abs(java.time.Duration.between(original, end).toMinutes());

            if (durationToStart <= durationToEnd) {
                return start.minusMinutes(1).toString();
            } else {
                return end.plusMinutes(1).toString();
            }
        } catch (Exception e) {
            return rule.getEndTime();
        }
    }

    private boolean isTimeInRange(LocalTime time, String startStr, String endStr) {
        try {
            LocalTime start = parseTime(startStr);
            LocalTime end = parseTime(endStr);

            if (isCrossDayRange(startStr, endStr)) {
                return !time.isBefore(start) || time.isBefore(end);
            } else {
                return !time.isBefore(start) && time.isBefore(end);
            }
        } catch (Exception e) {
            log.error("判断时间范围失败", e);
            return false;
        }
    }

    private boolean isCrossDayRange(String startStr, String endStr) {
        try {
            LocalTime start = parseTime(startStr);
            LocalTime end = parseTime(endStr);
            return !start.isBefore(end);
        } catch (Exception e) {
            return false;
        }
    }

    private LocalTime parseTime(String timeStr) {
        String[] parts = timeStr.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        return LocalTime.of(hour, minute);
    }

    @Transactional
    public QuietHourRule createRule(QuietHourRuleDTO dto) {
        validateTimeFormat(dto.getStartTime(), dto.getEndTime());

        QuietHourRule rule = new QuietHourRule();
        rule.setName(dto.getName());
        rule.setStartTime(normalizeTime(dto.getStartTime()));
        rule.setEndTime(normalizeTime(dto.getEndTime()));
        rule.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        rule.setCategory(dto.getCategory() != null ? dto.getCategory() : "自定义");
        rule.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : getNextSortOrder());

        this.save(rule);
        clearCache();
        return rule;
    }

    @Transactional
    public QuietHourRule updateRule(Long id, QuietHourRuleDTO dto) {
        QuietHourRule rule = this.getById(id);
        if (rule == null) {
            return null;
        }

        if (dto.getName() != null) {
            rule.setName(dto.getName());
        }
        if (dto.getStartTime() != null && dto.getEndTime() != null) {
            validateTimeFormat(dto.getStartTime(), dto.getEndTime());
            rule.setStartTime(normalizeTime(dto.getStartTime()));
            rule.setEndTime(normalizeTime(dto.getEndTime()));
        } else if (dto.getStartTime() != null) {
            validateTimeFormat(dto.getStartTime(), rule.getEndTime());
            rule.setStartTime(normalizeTime(dto.getStartTime()));
        } else if (dto.getEndTime() != null) {
            validateTimeFormat(rule.getStartTime(), dto.getEndTime());
            rule.setEndTime(normalizeTime(dto.getEndTime()));
        }
        if (dto.getEnabled() != null) {
            rule.setEnabled(dto.getEnabled());
        }
        if (dto.getCategory() != null) {
            rule.setCategory(dto.getCategory());
        }
        if (dto.getSortOrder() != null) {
            rule.setSortOrder(dto.getSortOrder());
        }

        this.updateById(rule);
        clearCache();
        return rule;
    }

    @Transactional
    public boolean deleteRule(Long id) {
        boolean result = this.removeById(id);
        if (result) {
            clearCache();
        }
        return result;
    }

    @Transactional
    public QuietHourRule toggleRule(Long id) {
        QuietHourRule rule = this.getById(id);
        if (rule == null) {
            return null;
        }
        rule.setEnabled(!(rule.getEnabled() != null && rule.getEnabled()));
        this.updateById(rule);
        clearCache();
        return rule;
    }

    private void validateTimeFormat(String startStr, String endStr) {
        try {
            parseTime(startStr);
            parseTime(endStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("时间格式错误，应为 HH:mm 格式");
        }
    }

    private String normalizeTime(String timeStr) {
        LocalTime time = parseTime(timeStr);
        return time.toString();
    }

    private Integer getNextSortOrder() {
        List<QuietHourRule> all = this.list();
        if (all.isEmpty()) {
            return 1;
        }
        return all.stream()
                .map(QuietHourRule::getSortOrder)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    private List<QuietHourRule> initDefaultRules() {
        List<QuietHourRule> defaults = new ArrayList<>();

        defaults.add(createRuleInternal("午休时段", "12:30", "14:00", true, "作息", 1));
        defaults.add(createRuleInternal("深夜勿扰", "23:00", "06:00", true, "作息", 2));
        defaults.add(createRuleInternal("深度工作", "09:30", "11:30", false, "专注", 3));
        defaults.add(createRuleInternal("下午专注", "15:00", "17:00", false, "专注", 4));

        log.info("初始化默认安静时段规则，共{}条", defaults.size());
        return defaults;
    }

    private QuietHourRule createRuleInternal(String name, String startTime, String endTime,
                                             boolean enabled, String category, int sortOrder) {
        QuietHourRule rule = new QuietHourRule();
        rule.setName(name);
        rule.setStartTime(startTime);
        rule.setEndTime(endTime);
        rule.setEnabled(enabled);
        rule.setCategory(category);
        rule.setSortOrder(sortOrder);
        this.save(rule);
        return rule;
    }

    private void clearCache() {
        redisTemplate.delete(QUIET_HOUR_CACHE_KEY);
        redisTemplate.delete(QUIET_HOUR_ENABLED_CACHE_KEY);
    }
}
