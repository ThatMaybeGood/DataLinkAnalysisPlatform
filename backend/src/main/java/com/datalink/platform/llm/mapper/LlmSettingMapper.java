package com.datalink.platform.llm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.llm.entity.LlmSetting;
import org.apache.ibatis.annotations.Mapper;

/**
 * 大模型配置 Mapper
 */
@Mapper
public interface LlmSettingMapper extends BaseMapper<LlmSetting> {
}
