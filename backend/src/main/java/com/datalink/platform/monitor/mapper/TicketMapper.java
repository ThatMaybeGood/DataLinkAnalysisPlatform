package com.datalink.platform.monitor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.datalink.platform.monitor.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单 Mapper
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
}
