package com.datalink.platform.monitor;

import com.datalink.platform.monitor.service.InterventionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 等级预警/干预服务测试（内存 H2 + Flyway V1~V5 种子数据）。
 * node 13=支付系统 L1、node 8=结算部门 L3。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_m2_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Transactional
class InterventionServiceTest {

    @Autowired
    private InterventionService interventionService;

    @Test
    void disposition_for_l1_contains_auto_action_ticket_notify() {
        String disposition = interventionService.dispositionFor("NODE", 13L);
        assertEquals("AUTO_ACTION,TICKET,NOTIFY", disposition, "L1 应含 AUTO_ACTION/TICKET/NOTIFY");
    }

    @Test
    void disposition_for_l3_is_notify() {
        String disposition = interventionService.dispositionFor("NODE", 8L);
        assertEquals("NOTIFY", disposition, "L3 应仅 NOTIFY");
    }
}
