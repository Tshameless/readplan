package com.readplan.module.shared.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.readplan.module.shared.store.entity.BookEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BookMapper extends BaseMapper<BookEntity> {
}
