package com.readplan.module.shared.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.readplan.module.shared.store.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
