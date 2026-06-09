package com.habit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.habit.entity.Checkin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface CheckinMapper extends BaseMapper<Checkin> {
    
    List<Checkin> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
