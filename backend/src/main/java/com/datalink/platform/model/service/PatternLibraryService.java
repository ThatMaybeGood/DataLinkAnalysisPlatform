package com.datalink.platform.model.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.datalink.platform.model.dto.PatternLibraryDTO;
import com.datalink.platform.model.entity.PatternLibrary;
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
 * 模式库服务（图来源 G5）
 */
@Service
@RequiredArgsConstructor
public class PatternLibraryService {

    private final PatternLibraryMapper patternLibraryMapper;

    /**
     * 新增模式
     */
    @Transactional
    public PatternLibrary save(PatternLibraryDTO dto) {
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
     * 列表查询
     */
    public List<PatternLibrary> list(String patternType, String keyword) {
        return patternLibraryMapper.selectList(Wrappers.lambdaQuery(PatternLibrary.class)
                .eq(StringUtils.hasText(patternType), PatternLibrary::getPatternType, patternType)
                .and(StringUtils.hasText(keyword), w -> w.like(PatternLibrary::getPatternKey, keyword)
                        .or()
                        .like(PatternLibrary::getPatternValue, keyword))
                .orderByDesc(PatternLibrary::getHitCount)
                .orderByDesc(PatternLibrary::getCreatedAt));
    }

    /**
     * 按 id 查询
     */
    public PatternLibrary getById(Long id) {
        return patternLibraryMapper.selectById(id);
    }

    /**
     * 按 patternKey 精确查询已确认模式
     */
    public List<PatternLibrary> findByKey(String patternKey) {
        return patternLibraryMapper.selectList(Wrappers.lambdaQuery(PatternLibrary.class)
                .eq(PatternLibrary::getPatternKey, patternKey)
                .eq(PatternLibrary::getConfirmed, 1));
    }

    /**
     * 模糊匹配模式
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
     * 命中次数 +1
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

    private String currentOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return "system";
    }
}
