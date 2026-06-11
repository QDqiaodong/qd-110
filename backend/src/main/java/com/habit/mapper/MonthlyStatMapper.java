package com.habit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.habit.entity.MonthlyStat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface MonthlyStatMapper extends BaseMapper<MonthlyStat> {
    
    MonthlyStat selectByYearMonth(@Param("year") Integer year, @Param("month") Integer month);
    
    List<MonthlyStat> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
