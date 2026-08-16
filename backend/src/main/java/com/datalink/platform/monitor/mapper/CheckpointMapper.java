package com.datalink.platform.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.monitor.entity.Checkpoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检测点 Mapper
 */
@Mapper
public interface CheckpointMapper extends BaseMapper<Checkpoint> {
}
