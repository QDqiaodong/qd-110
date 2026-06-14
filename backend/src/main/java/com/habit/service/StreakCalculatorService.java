package com.habit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.habit.dto.StreakBreakPoint;
import com.habit.dto.StreakResult;
import com.habit.dto.StreakSegment;
import com.habit.entity.Checkin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StreakCalculatorService {

    private static final String STREAK_CACHE_PREFIX = "streak:";
    private static final long CACHE_EXPIRE_MINUTES = 60;

    @Autowired
    private CheckinService checkinService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public StreakResult calculateForHabit(Long habitId) {
        return calculateForHabit(habitId, null, null);
    }

    public StreakResult calculateForHabitUntil(Long habitId, LocalDate endExclusive) {
        return calculateForHabit(habitId, null, endExclusive);
    }

    @SuppressWarnings("unchecked")
    public StreakResult calculateForHabit(Long habitId, LocalDate startDate, LocalDate endDate) {
        if (habitId == null) {
            return emptyResult(null);
        }

        String cacheKey = buildCacheKey(habitId, startDate, endDate);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof StreakResult) {
                return (StreakResult) cached;
            }
        } catch (SerializationException e) {
            redisTemplate.delete(cacheKey);
        }

        List<Checkin> completedCheckins = queryCompletedCheckins(habitId, startDate, endDate);
        StreakResult result = doCalculate(habitId, completedCheckins, startDate, endDate);

        redisTemplate.opsForValue().set(cacheKey, result, CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return result;
    }

    public StreakResult calculateForRange(Long habitId, LocalDate rangeStart, LocalDate rangeEnd) {
        if (habitId == null || rangeStart == null || rangeEnd == null) {
            return emptyResult(habitId);
        }
        return calculateForHabit(habitId, rangeStart, rangeEnd);
    }

    public Map<Long, StreakResult> calculateBatch(List<Long> habitIds) {
        Map<Long, StreakResult> results = new HashMap<>();
        if (habitIds == null || habitIds.isEmpty()) {
            return results;
        }
        for (Long habitId : habitIds) {
            results.put(habitId, calculateForHabit(habitId));
        }
        return results;
    }

    public int calculateCurrentStreakFast(Long habitId, LocalDate asOfDate) {
        List<Checkin> checkins = checkinService.list(
                new LambdaQueryWrapper<Checkin>()
                        .eq(Checkin::getHabitId, habitId)
                        .eq(Checkin::getCompleted, true)
                        .le(Checkin::getCheckinDate, asOfDate)
                        .orderByDesc(Checkin::getCheckinDate)
                        .last("LIMIT 365")
        );
        if (checkins.isEmpty()) {
            return 0;
        }

        List<LocalDate> dates = checkins.stream()
                .map(Checkin::getCheckinDate)
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        int streak = 0;
        LocalDate cursor = asOfDate;
        for (LocalDate d : dates) {
            if (d.isEqual(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else if (d.isEqual(cursor.minusDays(1)) && streak == 0) {
                streak = 1;
                cursor = d.minusDays(1);
            } else if (d.isBefore(cursor)) {
                break;
            }
        }
        return streak;
    }

    public void clearStreakCache(Long habitId) {
        if (habitId == null) return;
        String prefix = STREAK_CACHE_PREFIX + habitId;
        Set<String> keys = redisTemplate.keys(prefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    public void clearAllStreakCache() {
        Set<String> keys = redisTemplate.keys(STREAK_CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private StreakResult doCalculate(Long habitId, List<Checkin> completedCheckins,
                                     LocalDate startDate, LocalDate endDate) {
        StreakResult result = new StreakResult();
        result.setHabitId(habitId);

        if (completedCheckins.isEmpty()) {
            return emptyResult(habitId);
        }

        List<LocalDate> completedDates = completedCheckins.stream()
                .map(Checkin::getCheckinDate)
                .sorted()
                .collect(Collectors.toList());

        int totalCompleted = completedDates.size();
        LocalDate firstDate = completedDates.get(0);
        LocalDate lastDate = completedDates.get(completedDates.size() - 1);
        LocalDate today = endDate != null ? endDate.minusDays(1) : LocalDate.now();

        result.setTotalCompletedDays(totalCompleted);
        result.setFirstCheckinDate(firstDate);
        result.setLastCheckinDate(lastDate);

        List<StreakSegment> segments = new ArrayList<>();
        List<StreakBreakPoint> breakPoints = new ArrayList<>();

        LocalDate segStart = firstDate;
        LocalDate segPrev = firstDate;
        int maxStreak = 1;
        LocalDate maxStart = firstDate;
        LocalDate maxEnd = firstDate;

        for (int i = 1; i < completedDates.size(); i++) {
            LocalDate curr = completedDates.get(i);
            long diff = ChronoUnit.DAYS.between(segPrev, curr);
            if (diff == 1) {
                segPrev = curr;
            } else {
                int segDays = (int) ChronoUnit.DAYS.between(segStart, segPrev) + 1;
                segments.add(new StreakSegment(segStart, segPrev, segDays));

                if (segDays > maxStreak) {
                    maxStreak = segDays;
                    maxStart = segStart;
                    maxEnd = segPrev;
                }

                StreakBreakPoint bp = new StreakBreakPoint();
                bp.setPreviousCheckinDate(segPrev);
                bp.setNextCheckinDate(curr);
                int gapDays = (int) ChronoUnit.DAYS.between(segPrev, curr) - 1;
                bp.setGapDays(gapDays);
                bp.setBreakDate(segPrev.plusDays(1));
                breakPoints.add(bp);

                segStart = curr;
                segPrev = curr;
            }
        }

        int segDays = (int) ChronoUnit.DAYS.between(segStart, segPrev) + 1;
        segments.add(new StreakSegment(segStart, segPrev, segDays));
        if (segDays > maxStreak) {
            maxStreak = segDays;
            maxStart = segStart;
            maxEnd = segPrev;
        }

        result.setMaxStreak(maxStreak);
        result.setMaxStreakStartDate(maxStart);
        result.setMaxStreakEndDate(maxEnd);

        int currentStreak = 0;
        LocalDate currentStart = null;
        LocalDate currentEnd = null;

        if (completedDates.contains(today)) {
            currentEnd = today;
            currentStreak = 1;
            LocalDate d = today.minusDays(1);
            while (completedDates.contains(d)) {
                currentStreak++;
                d = d.minusDays(1);
            }
            currentStart = d.plusDays(1);
        } else if (completedDates.contains(today.minusDays(1))) {
            currentEnd = today.minusDays(1);
            currentStreak = 1;
            LocalDate d = today.minusDays(2);
            while (completedDates.contains(d)) {
                currentStreak++;
                d = d.minusDays(1);
            }
            currentStart = d.plusDays(1);
        }

        result.setCurrentStreak(currentStreak);
        result.setCurrentStreakStartDate(currentStart);
        result.setCurrentStreakEndDate(currentEnd);

        result.setSegments(segments);
        result.setBreakPoints(breakPoints);
        result.setTotalSegments(segments.size());
        result.setTotalBreakPoints(breakPoints.size());

        return result;
    }

    private List<Checkin> queryCompletedCheckins(Long habitId, LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<Checkin> wrapper = new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getHabitId, habitId)
                .eq(Checkin::getCompleted, true)
                .orderByAsc(Checkin::getCheckinDate);

        if (startDate != null) {
            wrapper.ge(Checkin::getCheckinDate, startDate);
        }
        if (endDate != null) {
            wrapper.lt(Checkin::getCheckinDate, endDate);
        }

        return checkinService.list(wrapper);
    }

    private StreakResult emptyResult(Long habitId) {
        StreakResult r = new StreakResult();
        r.setHabitId(habitId);
        r.setCurrentStreak(0);
        r.setMaxStreak(0);
        r.setTotalCompletedDays(0);
        r.setTotalSegments(0);
        r.setTotalBreakPoints(0);
        return r;
    }

    private String buildCacheKey(Long habitId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder(STREAK_CACHE_PREFIX);
        sb.append(habitId);
        if (startDate != null) {
            sb.append("_s:").append(startDate);
        }
        if (endDate != null) {
            sb.append("_e:").append(endDate);
        }
        return sb.toString();
    }
}
