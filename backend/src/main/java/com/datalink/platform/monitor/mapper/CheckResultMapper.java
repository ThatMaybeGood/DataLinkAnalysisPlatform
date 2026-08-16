package com.datalink.platform.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.monitor.entity.CheckResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 检测结果 Mapper
 */
@Mapper
public interface CheckResultMapper extends BaseMapper<CheckResult> {
}
