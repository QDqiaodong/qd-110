package com.habit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.habit.entity.Challenge;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChallengeMapper extends BaseMapper<Challenge> {
}
