package com.readplan.module.shared.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.readplan.module.shared.store.entity.NoteEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteMapper extends BaseMapper<NoteEntity> {
}
