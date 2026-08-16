package com.datalink.platform.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.model.entity.Alias;
import org.apache.ibatis.annotations.Mapper;

/**
 * 命名别名 Mapper
 */
@Mapper
public interface AliasMapper extends BaseMapper<Alias> {
}
