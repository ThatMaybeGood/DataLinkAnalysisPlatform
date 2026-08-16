package com.datalink.platform.model.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.model.dto.RouteVO;
import com.datalink.platform.model.dto.SaveRouteRequest;
import com.datalink.platform.model.entity.Route;
import com.datalink.platform.model.entity.RouteNode;
import com.datalink.platform.model.mapper.ProcessMapper;
import com.datalink.platform.model.mapper.RouteMapper;
import com.datalink.platform.model.mapper.RouteNodeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 建模域 CRUD 服务测试。
 * 每个测试方法 @Transactional 回滚，避免写入相互污染。
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:datalink_model_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1")
@Transactional
class ModelingServiceTest {

    @Autowired
    private ModelingService modelingService;
    @Autowired
    private RouteMapper routeMapper;
    @Autowired
    private RouteNodeMapper routeNodeMapper;
    @Autowired
    private ProcessMapper processMapper;

    private SaveRouteRequest buildRouteReq(List<Long> nodeIds) {
        SaveRouteRequest req = new SaveRouteRequest();
        req.setName("测试路线");
        req.setProcessId(2L);
        req.setPriority("ALTERNATE");
        req.setStatus("ACTIVE");
        req.setNodeIds(nodeIds);
        return req;
    }

    @Test
    void create_route_persists_route_nodes() {
        RouteVO vo = modelingService.createRoute(buildRouteReq(List.of(9L, 10L, 13L, 18L)));
        List<RouteNode> rns = routeNodeMapper.selectList(Wrappers.lambdaQuery(RouteNode.class)
                .eq(RouteNode::getRouteId, Long.valueOf(vo.getId())).orderByAsc(RouteNode::getSeq));
        assertEquals(4, rns.size(), "应写入 4 条路线站点");
        for (int i = 0; i < rns.size(); i++) {
            assertEquals(i + 1, rns.get(i).getSeq().intValue(), "seq 应从 1 起按序");
        }
    }

    @Test
    void update_route_replaces_route_nodes() {
        RouteVO created = modelingService.createRoute(buildRouteReq(List.of(9L, 10L, 13L, 18L)));
        Long routeId = Long.valueOf(created.getId());
        modelingService.updateRoute(routeId, buildRouteReq(List.of(9L, 18L)));
        List<RouteNode> rns = routeNodeMapper.selectList(Wrappers.lambdaQuery(RouteNode.class)
                .eq(RouteNode::getRouteId, routeId).orderByAsc(RouteNode::getSeq));
        assertEquals(2, rns.size(), "更新后应剩 2 条路线站点");
        assertEquals(9L, rns.get(0).getNodeId());
        assertEquals(18L, rns.get(1).getNodeId());
    }

    @Test
    void delete_process_cascades_routes() {
        modelingService.deleteProcess(2L);
        assertEquals(0, routeMapper.selectCount(Wrappers.lambdaQuery(Route.class)
                .eq(Route::getProcessId, 2L)).intValue(), "该流程下路线应清空");
        assertEquals(0, routeNodeMapper.selectCount(Wrappers.lambdaQuery(RouteNode.class)
                .eq(RouteNode::getRouteId, 3L)).intValue(), "级联路线的站点应清空");
        assertNull(processMapper.selectById(2L), "流程本身应删除");
    }
}
