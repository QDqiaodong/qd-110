package com.habit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.habit.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
}
