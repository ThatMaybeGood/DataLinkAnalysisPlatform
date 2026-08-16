package com.datalink.platform.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.monitor.entity.Alert;
import org.apache.ibatis.annotations.Mapper;

/**
 * 告警 Mapper
 */
@Mapper
public interface AlertMapper extends BaseMapper<Alert> {
}
