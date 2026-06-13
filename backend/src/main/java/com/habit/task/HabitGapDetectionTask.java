package com.habit.task;

import com.habit.service.HabitGapRuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HabitGapDetectionTask {

    private static final Logger log = LoggerFactory.getLogger(HabitGapDetectionTask.class);

    @Autowired
    private HabitGapRuleService habitGapRuleService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void detectGapsDaily() {
        log.info("开始执行每日空窗检测定时任务...");
        try {
            habitGapRuleService.detectGapsForAllRules();
            log.info("每日空窗检测定时任务执行完成");
        } catch (Exception e) {
            log.error("每日空窗检测定时任务执行失败", e);
        }
    }

    @Scheduled(cron = "0 0 */6 * * ?")
    public void detectGapsEvery6Hours() {
        log.debug("开始执行每6小时空窗检测定时任务...");
        try {
            habitGapRuleService.detectGapsForAllRules();
            log.debug("每6小时空窗检测定时任务执行完成");
        } catch (Exception e) {
            log.error("每6小时空窗检测定时任务执行失败", e);
        }
    }
}
