package com.datalink.platform.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.monitor.entity.Instance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 流程实例 Mapper
 */
@Mapper
public interface InstanceMapper extends BaseMapper<Instance> {
}
