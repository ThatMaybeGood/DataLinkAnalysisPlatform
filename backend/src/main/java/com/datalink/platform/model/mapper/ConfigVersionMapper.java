package com.datalink.platform.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.model.entity.ConfigVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 配置版本 Mapper
 */
@Mapper
public interface ConfigVersionMapper extends BaseMapper<ConfigVersion> {
}
