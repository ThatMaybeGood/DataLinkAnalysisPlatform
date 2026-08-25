package com.datalink.platform.engine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.datalink.platform.engine.entity.AnalysisTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 分析任务 Mapper
 */
@Mapper
public interface AnalysisTaskMapper extends BaseMapper<AnalysisTask> {

    /**
     * 轻量分页列表：不选 draft_snapshot 大列，仅查列表字段，按 id 倒序。
     *
     * @param connectorId 按首个来源过滤，为空查全部
     */
    @Select("<script>SELECT id, connector_id, connector_ids, connector_name, task_type, status, error_message, operator, created_at, finished_at " +
            "FROM analysis_task " +
            "<where><if test='connectorId != null'>connector_id = #{connectorId}</if></where> " +
            "ORDER BY id DESC</script>")
    IPage<AnalysisTask> selectPageLight(IPage<AnalysisTask> page, @Param("connectorId") Long connectorId);
}
