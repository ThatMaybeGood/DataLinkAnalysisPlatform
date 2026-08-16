package com.datalink.platform.model;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.PageResult;
import com.datalink.platform.model.dto.VersionVO;
import com.datalink.platform.model.entity.ConfigVersion;
import com.datalink.platform.model.mapper.ConfigVersionMapper;
import com.datalink.platform.model.service.ConfigVersionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置版本留痕服务测试（内存 H2 + Flyway V1~V5 种子数据）。
 * 每个测试方法事务回滚，避免写入相互污染。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_m2_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Transactional
class ConfigVersionServiceTest {

    @Autowired
    private ConfigVersionService configVersionService;
    @Autowired
    private ConfigVersionMapper configVersionMapper;

    @Test
    void record_increments_version() {
        configVersionService.record("PROCESS", 2L, "{\"name\":\"付款流程\"}", "新建流程", "system");
        configVersionService.record("PROCESS", 2L, "{\"name\":\"付款流程\"}", "更新流程", "system");

        List<ConfigVersion> list = configVersionMapper.selectList(Wrappers.lambdaQuery(ConfigVersion.class)
                .eq(ConfigVersion::getTargetType, "PROCESS")
                .eq(ConfigVersion::getTargetId, 2L)
                .orderByAsc(ConfigVersion::getVersion));
        assertEquals(2, list.size(), "同一目标应留存 2 个版本");
        assertEquals(1, list.get(0).getVersion().intValue(), "首个版本应为 1");
        assertEquals(2, list.get(1).getVersion().intValue(), "再次记录版本号应递增");
        assertEquals("PUBLISHED", list.get(0).getStatus(), "状态应默认 PUBLISHED");
    }

    @Test
    void page_returns_target_name() {
        configVersionService.record("PROCESS", 2L, "{}", "新建流程", "system");

        PageResult<VersionVO> page = configVersionService.page(1, 20, "PROCESS");
        assertTrue(page.getTotal() >= 1, "按 PROCESS 过滤应命中至少 1 条");
        VersionVO vo = page.getRecords().get(0);
        assertEquals("付款流程", vo.getTargetName(), "process id=2 应装配为「付款流程」");
        assertEquals("PROCESS", vo.getTargetType());
    }
}
