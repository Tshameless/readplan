package com.readplan.module.shared.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.readplan.module.shared.store.entity.ReadingPlanEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReadingPlanMapper extends BaseMapper<ReadingPlanEntity> {
}
