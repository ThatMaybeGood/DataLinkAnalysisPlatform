package com.datalink.platform.model.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.model.dto.CorrectionRecordDTO;
import com.datalink.platform.model.dto.PatternLibraryDTO;
import com.datalink.platform.model.entity.CorrectionRecord;
import com.datalink.platform.model.entity.PatternLibrary;
import com.datalink.platform.model.mapper.CorrectionRecordMapper;
import com.datalink.platform.model.mapper.PatternLibraryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 校正记录服务（图来源 G5）
 */
@Service
@RequiredArgsConstructor
public class CorrectionRecordService {

    private final CorrectionRecordMapper correctionRecordMapper;
    private final PatternLibraryMapper patternLibraryMapper;

    /**
     * 提交校正记录；若请求要求沉淀为模式，则同时写入模式库。
     */
    @Transactional
    public CorrectionRecord submit(CorrectionRecordDTO dto) {
        CorrectionRecord record = new CorrectionRecord();
        record.setTargetType(dto.getTargetType());
        record.setTargetId(dto.getTargetId());
        record.setTargetName(dto.getTargetName());
        record.setOperation(dto.getOperation());
        record.setOldValue(dto.getOldValue());
        record.setNewValue(dto.getNewValue());
        record.setMergeTargetId(dto.getMergeTargetId());
        record.setReorderNodeIds(dto.getReorderNodeIds());
        record.setStatus("PENDING");
        record.setSource("MANUAL");
        record.setOperator(currentOperator());
        record.setRemark(dto.getRemark());
        correctionRecordMapper.insert(record);

        if (Boolean.TRUE.equals(dto.getSavePattern())) {
            PatternLibraryDTO p = new PatternLibraryDTO();
            p.setPatternType(StringUtils.hasText(dto.getPatternType()) ? dto.getPatternType() : "NODE_NAME");
            p.setPatternKey(buildPatternKey(record));
            p.setPatternValue(record.getNewValue());
            p.setSourceType(record.getTargetType());
            p.setSourceId(record.getTargetId());
            p.setSourceOperation(record.getOperation());
            savePattern(p);
        }
        return record;
    }

    /**
     * 查询某对象的校正历史（创建时间倒序）
     */
    public List<CorrectionRecord> listByTarget(String targetType, String targetId) {
        return correctionRecordMapper.selectList(Wrappers.lambdaQuery(CorrectionRecord.class)
                .eq(CorrectionRecord::getTargetType, targetType)
                .eq(CorrectionRecord::getTargetId, targetId)
                .orderByDesc(CorrectionRecord::getCreatedAt));
    }

    /**
     * 确认一条校正生效，状态由 PENDING 变为 APPLIED
     */
    @Transactional
    public CorrectionRecord confirm(Long id) {
        CorrectionRecord record = correctionRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "校正记录不存在");
        }
        if (!"PENDING".equals(record.getStatus())) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "只能确认待处理记录");
        }
        record.setStatus("APPLIED");
        record.setUpdatedAt(LocalDateTime.now());
        correctionRecordMapper.updateById(record);
        return record;
    }

    /**
     * 手动新增模式库条目
     */
    @Transactional
    public PatternLibrary savePattern(PatternLibraryDTO dto) {
        PatternLibrary p = new PatternLibrary();
        p.setPatternType(dto.getPatternType());
        p.setPatternKey(dto.getPatternKey());
        p.setPatternValue(dto.getPatternValue());
        p.setSourceType(dto.getSourceType());
        p.setSourceId(dto.getSourceId());
        p.setSourceOperation(dto.getSourceOperation());
        p.setHitCount(0);
        p.setConfirmed(1);
        p.setCreatedBy(currentOperator());
        patternLibraryMapper.insert(p);
        return p;
    }

    /**
     * 模式库列表（按类型/关键词过滤，命中次数倒序）
     */
    public List<PatternLibrary> listPatterns(String patternType, String keyword) {
        return patternLibraryMapper.selectList(Wrappers.lambdaQuery(PatternLibrary.class)
                .eq(StringUtils.hasText(patternType), PatternLibrary::getPatternType, patternType)
                .and(StringUtils.hasText(keyword), w -> w.like(PatternLibrary::getPatternKey, keyword)
                        .or()
                        .like(PatternLibrary::getPatternValue, keyword))
                .orderByDesc(PatternLibrary::getHitCount)
                .orderByDesc(PatternLibrary::getCreatedAt));
    }

    /**
     * 从已确认的校正记录沉淀模式
     */
    @Transactional
    public PatternLibrary learnFrom(CorrectionRecord record) {
        PatternLibraryDTO dto = new PatternLibraryDTO();
        dto.setPatternType(patternTypeOf(record.getTargetType()));
        dto.setPatternKey(buildPatternKey(record));
        dto.setPatternValue(record.getNewValue());
        dto.setSourceType(record.getTargetType());
        dto.setSourceId(record.getTargetId());
        dto.setSourceOperation(record.getOperation());
        return savePattern(dto);
    }

    /**
     * 按 patternKey 查询已确认模式
     */
    public List<PatternLibrary> findByKey(String patternKey) {
        return patternLibraryMapper.selectList(Wrappers.lambdaQuery(PatternLibrary.class)
                .eq(PatternLibrary::getPatternKey, patternKey)
                .eq(PatternLibrary::getConfirmed, 1));
    }

    /**
     * 模糊匹配模式（用于二次识别自动应用）
     */
    public List<PatternLibrary> findPatterns(String patternType, String input) {
        return patternLibraryMapper.selectList(Wrappers.lambdaQuery(PatternLibrary.class)
                .eq(PatternLibrary::getConfirmed, 1)
                .eq(StringUtils.hasText(patternType), PatternLibrary::getPatternType, patternType)
                .and(StringUtils.hasText(input), w -> w.like(PatternLibrary::getPatternKey, input)
                        .or()
                        .like(PatternLibrary::getPatternValue, input))
                .orderByDesc(PatternLibrary::getHitCount));
    }

    /**
     * 增加模式命中次数
     */
    @Transactional
    public void hit(PatternLibrary pattern) {
        if (pattern == null || pattern.getId() == null) {
            return;
        }
        pattern.setHitCount(pattern.getHitCount() == null ? 1 : pattern.getHitCount() + 1);
        pattern.setUpdatedAt(LocalDateTime.now());
        patternLibraryMapper.updateById(pattern);
    }

    private String buildPatternKey(CorrectionRecord record) {
        if (StringUtils.hasText(record.getOldValue())) {
            return record.getTargetType() + ":" + record.getOldValue();
        }
        return record.getTargetType() + ":" + record.getTargetId();
    }

    private String patternTypeOf(String targetType) {
        return switch (targetType == null ? "" : targetType) {
            case "EDGE" -> "EDGE_NAME";
            case "ROUTE" -> "ROUTE_TEMPLATE";
            default -> "NODE_NAME";
        };
    }

    private String currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return "system";
    }
}
