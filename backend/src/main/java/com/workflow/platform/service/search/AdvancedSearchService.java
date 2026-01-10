package com.workflow.platform.service.search;

import com.workflow.platform.model.dto.SearchDTO;
import com.workflow.platform.model.dto.SearchResultDTO;
import com.workflow.platform.model.entity.WorkflowEntity;
import com.workflow.platform.repository.WorkflowRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import javax.persistence.criteria.Predicate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 高级搜索服务 - 提供全文搜索、语义搜索、智能推荐等功能
 */
@Slf4j
@Service
public class AdvancedSearchService {

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private SearchIndexManager searchIndexManager;

    @Autowired
    private SearchRankingService searchRankingService;

    @Autowired
    private SearchSuggestionService searchSuggestionService;

    /**
     * 执行高级搜索
     */
    public SearchResultDTO advancedSearch(SearchDTO searchDTO) {
        log.info("执行高级搜索: {}", searchDTO.getQuery());

        SearchResultDTO result = new SearchResultDTO();
        result.setSearchId(generateSearchId());
        result.setQuery(searchDTO.getQuery());
        result.setTimestamp(System.currentTimeMillis());

        try {
            // 1. 查询预处理
            SearchQuery processedQuery = preprocessSearchQuery(searchDTO);

            // 2. 多维度搜索
            List<SearchResultItem> searchResults = new ArrayList<>();

            // 全文搜索
            if (processedQuery.isFullTextSearch()) {
                List<SearchResultItem> fullTextResults = performFullTextSearch(processedQuery);
                searchResults.addAll(fullTextResults);
            }

            // 语义搜索
            if (processedQuery.isSemanticSearch()) {
                List<SearchResultItem> semanticResults = performSemanticSearch(processedQuery);
                searchResults.addAll(semanticResults);
            }

            // 字段搜索
            if (processedQuery.hasFieldFilters()) {
                List<SearchResultItem> fieldResults = performFieldSearch(processedQuery);
                searchResults.addAll(fieldResults);
            }

            // 3. 结果去重和合并
            List<SearchResultItem> mergedResults = mergeAndDeduplicateResults(searchResults);

            // 4. 智能排序
            List<SearchResultItem> rankedResults = searchRankingService.rankResults(
                    mergedResults, processedQuery, searchDTO.getUserId());

            // 5. 分页处理
            List<SearchResultItem> paginatedResults = applyPagination(
                    rankedResults, searchDTO.getPage(), searchDTO.getSize());

            // 6. 丰富结果数据
            List<SearchResultItem> enrichedResults = enrichSearchResults(paginatedResults);

            // 7. 设置结果
            result.setResults(enrichedResults);
            result.setTotalResults(rankedResults.size());
            result.setPage(searchDTO.getPage());
            result.setPageSize(searchDTO.getSize());
            result.setTotalPages((int) Math.ceil((double) rankedResults.size() / searchDTO.getSize()));

            // 8. 生成搜索建议
            List<String> suggestions = searchSuggestionService.generateSuggestions(
                    searchDTO.getQuery(), searchDTO.getUserId());
            result.setSuggestions(suggestions);

            // 9. 生成搜索摘要
            Map<String, Object> summary = generateSearchSummary(searchDTO, result);
            result.setSummary(summary);

            // 10. 记录搜索历史
            recordSearchHistory(searchDTO, result);

            log.info("高级搜索完成: {}，找到 {} 个结果",
                    searchDTO.getQuery(), result.getTotalResults());

        } catch (Exception e) {
            log.error("高级搜索失败: {}，错误: {}", searchDTO.getQuery(), e.getMessage(), e);
            result.setError("搜索失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 实时搜索建议
     */
    public List<SearchSuggestion> getSearchSuggestions(String query, String userId, int limit) {
        return searchSuggestionService.getSuggestions(query, userId, limit);
    }

    /**
     * 搜索自动补全
     */
    public List<String> autoComplete(String prefix, String field, int limit) {
        return searchIndexManager.autoComplete(prefix, field, limit);
    }

    /**
     * 相似工作流搜索
     */
    public List<WorkflowEntity> findSimilarWorkflows(String workflowId, int limit) {
        try {
            // 获取目标工作流
            Optional<WorkflowEntity> targetWorkflow = workflowRepository.findById(workflowId);
            if (!targetWorkflow.isPresent()) {
                return Collections.emptyList();
            }

            // 提取特征向量
            WorkflowFeatureVector targetVector = extractWorkflowFeatures(targetWorkflow.get());

            // 搜索相似工作流
            List<WorkflowSimilarity> similarities = new ArrayList<>();

            // 基于内容的相似度
            List<WorkflowSimilarity> contentSimilarities =
                    findSimilarByContent(targetWorkflow.get(), targetVector, limit * 2);
            similarities.addAll(contentSimilarities);

            // 基于结构的相似度
            List<WorkflowSimilarity> structureSimilarities =
                    findSimilarByStructure(targetWorkflow.get(), limit);
            similarities.addAll(structureSimilarities);

            // 基于使用模式的相似度
            List<WorkflowSimilarity> usageSimilarities =
                    findSimilarByUsagePattern(targetWorkflow.get(), limit);
            similarities.addAll(usageSimilarities);

            // 合并和去重
            Map<String, WorkflowSimilarity> merged = new HashMap<>();
            for (WorkflowSimilarity similarity : similarities) {
                merged.merge(similarity.getWorkflowId(), similarity,
                        (s1, s2) -> s1.getScore() > s2.getScore() ? s1 : s2);
            }

            // 排序和限制
            List<WorkflowSimilarity> topSimilarities = merged.values().stream()
                    .sorted(Comparator.comparing(WorkflowSimilarity::getScore).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());

            // 获取工作流实体
            List<String> workflowIds = topSimilarities.stream()
                    .map(WorkflowSimilarity::getWorkflowId)
                    .collect(Collectors.toList());

            return workflowRepository.findAllById(workflowIds);

        } catch (Exception e) {
            log.error("查找相似工作流失败: {}，错误: {}", workflowId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 高级过滤搜索
     */
    public Page<WorkflowEntity> advancedFilterSearch(Map<String, Object> filters,
                                                     int page, int size,
                                                     String sortField, String sortDirection) {
        Specification<WorkflowEntity> spec = buildFilterSpecification(filters);

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortField);
        Pageable pageable = PageRequest.of(page, size, sort);

        return workflowRepository.findAll(spec, pageable);
    }

    /**
     * 获取搜索统计
     */
    public SearchStatistics getSearchStatistics(Date startDate, Date endDate, String userId) {
        SearchStatistics stats = new SearchStatistics();
        stats.setPeriodStart(startDate);
        stats.setPeriodEnd(endDate);

        // 获取搜索历史数据
        List<SearchHistory> searchHistory = getSearchHistory(startDate, endDate, userId);

        // 计算统计
        stats.setTotalSearches(searchHistory.size());
        stats.setUniqueQueries(calculateUniqueQueries(searchHistory));
        stats.setAverageResultsPerSearch(calculateAverageResults(searchHistory));
        stats.setZeroResultSearches(countZeroResultSearches(searchHistory));

        // 热门搜索
        stats.setPopularQueries(calculatePopularQueries(searchHistory, 10));

        // 搜索成功率
        stats.setSuccessRate(calculateSuccessRate(searchHistory));

        // 搜索时间分布
        stats.setTimeDistribution(calculateTimeDistribution(searchHistory));

        return stats;
    }

    /**
     * 重新构建搜索索引
     */
    public boolean rebuildSearchIndex(boolean fullRebuild) {
        try {
            log.info("开始重建搜索索引，完全重建: {}", fullRebuild);

            if (fullRebuild) {
                // 清除现有索引
                searchIndexManager.clearIndex();
            }

            // 分批索引数据
            int batchSize = 100;
            int totalIndexed = 0;
            int page = 0;

            while (true) {
                Pageable pageable = PageRequest.of(page, batchSize);
                Page<WorkflowEntity> workflows = workflowRepository.findAll(pageable);

                if (!workflows.hasContent()) {
                    break;
                }

                // 索引当前批次
                searchIndexManager.indexBatch(workflows.getContent());
                totalIndexed += workflows.getNumberOfElements();

                log.debug("索引进度: {} 个工作流", totalIndexed);

                if (!workflows.hasNext()) {
                    break;
                }

                page++;
            }

            // 优化索引
            searchIndexManager.optimizeIndex();

            log.info("搜索索引重建完成，共索引 {} 个工作流", totalIndexed);
            return true;

        } catch (Exception e) {
            log.error("重建搜索索引失败: {}", e.getMessage(), e);
            return false;
        }
    }

    // ========== 私有方法 ==========

    private SearchQuery preprocessSearchQuery(SearchDTO searchDTO) {
        SearchQuery query = new SearchQuery();
        query.setOriginalQuery(searchDTO.getQuery());
        query.setUserId(searchDTO.getUserId());

        // 查询解析
        ParsedQuery parsed = parseQuery(searchDTO.getQuery());
        query.setParsedQuery(parsed);

        // 查询扩展
        List<String> expandedTerms = expandQueryTerms(parsed.getKeywords());
        query.setExpandedTerms(expandedTerms);

        // 确定搜索类型
        determineSearchTypes(query, searchDTO);

        // 设置权重
        Map<String, Double> weights = calculateFieldWeights(searchDTO);
        query.setFieldWeights(weights);

        return query;
    }

    private ParsedQuery parseQuery(String query) {
        ParsedQuery parsed = new ParsedQuery();

        // 提取关键词
        List<String> keywords = extractKeywords(query);
        parsed.setKeywords(keywords);

        // 提取操作符
        List<QueryOperator> operators = extractOperators(query);
        parsed.setOperators(operators);

        // 提取字段过滤
        Map<String, String> fieldFilters = extractFieldFilters(query);
        parsed.setFieldFilters(fieldFilters);

        // 识别查询类型
        String queryType = identifyQueryType(query);
        parsed.setQueryType(queryType);

        return parsed;
    }

    private List<String> extractKeywords(String query) {
        // 简单关键词提取
        return Arrays.stream(query.split("\\s+"))
                .filter(term -> !term.isEmpty() && term.length() > 1)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    private List<QueryOperator> extractOperators(String query) {
        List<QueryOperator> operators = new ArrayList<>();

        // 识别AND、OR、NOT等操作符
        if (query.contains(" AND ")) {
            operators.add(QueryOperator.AND);
        }
        if (query.contains(" OR ")) {
            operators.add(QueryOperator.OR);
        }
        if (query.contains(" NOT ") || query.contains("-")) {
            operators.add(QueryOperator.NOT);
        }

        return operators;
    }

    private Map<String, String> extractFieldFilters(String query) {
        Map<String, String> filters = new HashMap<>();

        // 提取field:value格式的过滤
        String[] parts = query.split("\\s+");
        for (String part : parts) {
            if (part.contains(":")) {
                String[] fieldValue = part.split(":", 2);
                if (fieldValue.length == 2) {
                    filters.put(fieldValue[0].toLowerCase(), fieldValue[1]);
                }
            }
        }

        return filters;
    }

    private String identifyQueryType(String query) {
        if (query.startsWith("\"") && query.endsWith("\"")) {
            return "exact";
        }

        if (query.contains("*") || query.contains("?")) {
            return "wildcard";
        }

        if (query.contains("~")) {
            return "fuzzy";
        }

        if (query.matches(".*[A-Z].*") && !query.equals(query.toUpperCase())) {
            return "case_sensitive";
        }

        return "standard";
    }

    private List<String> expandQueryTerms(List<String> keywords) {
        List<String> expanded = new ArrayList<>(keywords);

        // 同义词扩展
        for (String keyword : keywords) {
            List<String> synonyms = getSynonyms(keyword);
            expanded.addAll(synonyms);
        }

        // 词干提取
        for (String keyword : keywords) {
            String stem = getStem(keyword);
            if (!expanded.contains(stem)) {
                expanded.add(stem);
            }
        }

        return expanded;
    }

    private List<String> getSynonyms(String word) {
        // 同义词词典
        Map<String, List<String>> synonymDict = new HashMap<>();
        synonymDict.put("workflow", Arrays.asList("process", "pipeline", "flow"));
        synonymDict.put("node", Arrays.asList("step", "stage", "task"));
        synonymDict.put("validation", Arrays.asList("check", "verify", "validate"));

        return synonymDict.getOrDefault(word.toLowerCase(), Collections.emptyList());
    }

    private String getStem(String word) {
        // 简单的词干提取
        if (word.endsWith("ing")) {
            return word.substring(0, word.length() - 3);
        }
        if (word.endsWith("ed")) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s")) {
            return word.substring(0, word.length() - 1);
        }
        return word;
    }

    private void determineSearchTypes(SearchQuery query, SearchDTO searchDTO) {
        // 根据查询内容确定搜索类型
        query.setFullTextSearch(true); // 默认启用全文搜索

        // 检查是否需要语义搜索
        if (searchDTO.isSemanticSearch() ||
                query.getParsedQuery().getKeywords().size() >= 3) {
            query.setSemanticSearch(true);
        }

        // 检查是否有字段过滤
        if (!query.getParsedQuery().getFieldFilters().isEmpty()) {
            query.setFieldFilters(query.getParsedQuery().getFieldFilters());
        }
    }

    private Map<String, Double> calculateFieldWeights(SearchDTO searchDTO) {
        Map<String, Double> weights = new HashMap<>();

        // 默认权重
        weights.put("name", 2.0);
        weights.put("description", 1.5);
        weights.put("category", 1.2);
        weights.put("tags", 1.0);
        weights.put("content", 0.8);

        // 根据搜索类型调整权重
        if ("exact".equals(searchDTO.getSearchType())) {
            weights.put("name", 3.0);
        } else if ("fuzzy".equals(searchDTO.getSearchType())) {
            weights.put("content", 1.5);
        }

        return weights;
    }

    private List<SearchResultItem> performFullTextSearch(SearchQuery query) {
        return searchIndexManager.search(
                query.getExpandedTerms(),
                query.getParsedQuery().getOperators(),
                query.getFieldWeights(),
                query.getUserId()
        );
    }

    private List<SearchResultItem> performSemanticSearch(SearchQuery query) {
        // 使用语义搜索算法
        List<SearchResultItem> results = new ArrayList<>();

        // 获取查询的语义向量
        double[] queryVector = getSemanticVector(query.getOriginalQuery());

        // 搜索相似的文档向量
        List<DocumentSimilarity> similarities =
                searchIndexManager.findSemanticSimilarities(queryVector, 50);

        // 转换为搜索结果
        for (DocumentSimilarity similarity : similarities) {
            if (similarity.getScore() > 0.3) { // 相似度阈值
                SearchResultItem item = new SearchResultItem();
                item.setDocumentId(similarity.getDocumentId());
                item.setScore(similarity.getScore());
                item.setSearchType("semantic");
                results.add(item);
            }
        }

        return results;
    }

    private List<SearchResultItem> performFieldSearch(SearchQuery query) {
        List<SearchResultItem> results = new ArrayList<>();

        // 构建字段查询
        Map<String, Object> fieldQueries = new HashMap<>();
        for (Map.Entry<String, String> entry : query.getFieldFilters().entrySet()) {
            fieldQueries.put(entry.getKey(), entry.getValue());
        }

        // 执行字段搜索
        List<WorkflowEntity> workflows = workflowRepository.findByFields(fieldQueries);

        // 转换为搜索结果
        for (WorkflowEntity workflow : workflows) {
            SearchResultItem item = new SearchResultItem();
            item.setDocumentId(workflow.getId());
            item.setScore(1.0);
            item.setSearchType("field");
            results.add(item);
        }

        return results;
    }

    private double[] getSemanticVector(String query) {
        // 简化的语义向量生成
        // 在实际应用中，应使用BERT等预训练模型
        double[] vector = new double[300]; // 假设300维向量

        // 基于词频的简单向量
        String[] words = query.toLowerCase().split("\\s+");
        for (String word : words) {
            int hash = Math.abs(word.hashCode()) % vector.length;
            vector[hash] += 1.0;
        }

        // 归一化
        double norm = Math.sqrt(Arrays.stream(vector).map(v -> v * v).sum());
        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    private List<SearchResultItem> mergeAndDeduplicateResults(List<SearchResultItem> results) {
        Map<String, SearchResultItem> merged = new HashMap<>();

        for (SearchResultItem item : results) {
            String docId = item.getDocumentId();
            if (merged.containsKey(docId)) {
                // 合并得分
                SearchResultItem existing = merged.get(docId);
                existing.setScore(Math.max(existing.getScore(), item.getScore()));

                // 合并搜索类型
                Set<String> types = new HashSet<>(
                        Arrays.asList(existing.getSearchType().split(",")));
                types.add(item.getSearchType());
                existing.setSearchType(String.join(",", types));
            } else {
                merged.put(docId, item);
            }
        }

        return new ArrayList<>(merged.values());
    }

    private List<SearchResultItem> applyPagination(List<SearchResultItem> results,
                                                   int page, int size) {
        int start = page * size;
        int end = Math.min(start + size, results.size());

        if (start >= results.size()) {
            return Collections.emptyList();
        }

        return results.subList(start, end);
    }

    private List<SearchResultItem> enrichSearchResults(List<SearchResultItem> results) {
        List<String> docIds = results.stream()
                .map(SearchResultItem::getDocumentId)
                .collect(Collectors.toList());

        // 获取工作流详细信息
        List<WorkflowEntity> workflows = workflowRepository.findAllById(docIds);
        Map<String, WorkflowEntity> workflowMap = workflows.stream()
                .collect(Collectors.toMap(WorkflowEntity::getId, w -> w));

        // 丰富结果数据
        for (SearchResultItem item : results) {
            WorkflowEntity workflow = workflowMap.get(item.getDocumentId());
            if (workflow != null) {
                item.setTitle(workflow.getName());
                item.setDescription(workflow.getDescription());
                item.setCategory(workflow.getCategory());
                item.setTags(workflow.getTags());
                item.setCreatedTime(workflow.getCreatedTime());
                item.setUpdatedTime(workflow.getUpdatedTime());

                // 生成摘要
                String snippet = generateSnippet(workflow, item.getMatchedTerms());
                item.setSnippet(snippet);
            }
        }

        return results;
    }

    private String generateSnippet(WorkflowEntity workflow, List<String> matchedTerms) {
        // 生成搜索结果摘要
        String text = workflow.getDescription() != null ? workflow.getDescription() : "";

        if (text.length() > 200) {
            // 找到第一个匹配项的位置
            int firstMatch = -1;
            for (String term : matchedTerms) {
                int index = text.toLowerCase().indexOf(term.toLowerCase());
                if (index >= 0 && (firstMatch < 0 || index < firstMatch)) {
                    firstMatch = index;
                }
            }

            if (firstMatch >= 0) {
                int start = Math.max(0, firstMatch - 50);
                int end = Math.min(text.length(), firstMatch + 150);
                String snippet = text.substring(start, end);

                if (start > 0) {
                    snippet = "..." + snippet;
                }
                if (end < text.length()) {
                    snippet = snippet + "...";
                }

                // 高亮匹配项
                for (String term : matchedTerms) {
                    snippet = snippet.replaceAll("(?i)(" + term + ")", "<em>$1</em>");
                }

                return snippet;
            }
        }

        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    private Map<String, Object> generateSearchSummary(SearchDTO searchDTO, SearchResultDTO result) {
        Map<String, Object> summary = new HashMap<>();

        summary.put("query", searchDTO.getQuery());
        summary.put("searchTime", System.currentTimeMillis() - result.getTimestamp());
        summary.put("searchTypes", extractSearchTypes(result.getResults()));
        summary.put("categories", extractCategories(result.getResults()));

        // 搜索质量评估
        double qualityScore = evaluateSearchQuality(result);
        summary.put("qualityScore", qualityScore);

        return summary;
    }

    private Set<String> extractSearchTypes(List<SearchResultItem> results) {
        return results.stream()
                .map(SearchResultItem::getSearchType)
                .flatMap(type -> Arrays.stream(type.split(",")))
                .collect(Collectors.toSet());
    }

    private Set<String> extractCategories(List<SearchResultItem> results) {
        return results.stream()
                .map(SearchResultItem::getCategory)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private double evaluateSearchQuality(SearchResultDTO result) {
        // 评估搜索质量
        double quality = 0.0;

        // 结果相关性
        quality += result.getResults().stream()
                .mapToDouble(SearchResultItem::getScore)
                .average()
                .orElse(0.0) * 0.5;

        // 结果多样性
        Set<String> categories = extractCategories(result.getResults());
        quality += Math.min(categories.size() / 5.0, 1.0) * 0.3;

        // 结果数量合适度
        double resultCountScore = Math.min(result.getTotalResults() / 100.0, 1.0);
        quality += resultCountScore * 0.2;

        return quality;
    }

    private void recordSearchHistory(SearchDTO searchDTO, SearchResultDTO result) {
        SearchHistory history = new SearchHistory();
        history.setSearchId(result.getSearchId());
        history.setQuery(searchDTO.getQuery());
        history.setUserId(searchDTO.getUserId());
        history.setTimestamp(System.currentTimeMillis());
        history.setResultCount(result.getTotalResults());
        history.setZeroResults(result.getTotalResults() == 0);
        history.setFilters(searchDTO.getFilters());

        // 保存搜索历史
        searchIndexManager.saveSearchHistory(history);
    }

    private String generateSearchId() {
        return "search_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8);
    }

    private WorkflowFeatureVector extractWorkflowFeatures(WorkflowEntity workflow) {
        WorkflowFeatureVector vector = new WorkflowFeatureVector();

        // 提取文本特征
        vector.setTextFeatures(extractTextFeatures(workflow));

        // 提取结构特征
        vector.setStructureFeatures(extractStructureFeatures(workflow));

        // 提取元数据特征
        vector.setMetadataFeatures(extractMetadataFeatures(workflow));

        return vector;
    }

    private double[] extractTextFeatures(WorkflowEntity workflow) {
        // 提取文本特征向量
        String text = workflow.getName() + " " +
                (workflow.getDescription() != null ? workflow.getDescription() : "");

        // 简化的TF-IDF特征
        return getSemanticVector(text);
    }

    private Map<String, Object> extractStructureFeatures(WorkflowEntity workflow) {
        Map<String, Object> features = new HashMap<>();

        // 解析工作流结构
        // 这里简化实现
        features.put("nodeCount", 0);
        features.put("depth", 0);
        features.put("branchingFactor", 0);

        return features;
    }

    private Map<String, Object> extractMetadataFeatures(WorkflowEntity workflow) {
        Map<String, Object> features = new HashMap<>();

        features.put("category", workflow.getCategory());
        features.put("tags", workflow.getTags());
        features.put("createdTime", workflow.getCreatedTime().getTime());
        features.put("updateCount", workflow.getVersion());

        return features;
    }

    private List<WorkflowSimilarity> findSimilarByContent(WorkflowEntity target,
                                                          WorkflowFeatureVector targetVector,
                                                          int limit) {
        List<WorkflowSimilarity> similarities = new ArrayList<>();

        // 基于内容相似度搜索
        // 这里简化实现

        return similarities;
    }

    private List<WorkflowSimilarity> findSimilarByStructure(WorkflowEntity target, int limit) {
        List<WorkflowSimilarity> similarities = new ArrayList<>();

        // 基于结构相似度搜索
        // 这里简化实现

        return similarities;
    }

    private List<WorkflowSimilarity> findSimilarByUsagePattern(WorkflowEntity target, int limit) {
        List<WorkflowSimilarity> similarities = new ArrayList<>();

        // 基于使用模式相似度搜索
        // 这里简化实现

        return similarities;
    }

    private Specification<WorkflowEntity> buildFilterSpecification(Map<String, Object> filters) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value != null) {
                    switch (key) {
                        case "name":
                            predicates.add(criteriaBuilder.like(
                                    root.get("name"), "%" + value + "%"));
                            break;
                        case "category":
                            predicates.add(criteriaBuilder.equal(
                                    root.get("category"), value));
                            break;
                        case "tags":
                            predicates.add(criteriaBuilder.like(
                                    root.get("tags"), "%" + value + "%"));
                            break;
                        case "createdBy":
                            predicates.add(criteriaBuilder.equal(
                                    root.get("createdBy"), value));
                            break;
                        case "createdTimeFrom":
                            predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                                    root.get("createdTime"), (Date) value));
                            break;
                        case "createdTimeTo":
                            predicates.add(criteriaBuilder.lessThanOrEqualTo(
                                    root.get("createdTime"), (Date) value));
                            break;
                        case "status":
                            predicates.add(criteriaBuilder.equal(
                                    root.get("status"), value));
                            break;
                    }
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<SearchHistory> getSearchHistory(Date startDate, Date endDate, String userId) {
        // 获取搜索历史
        return searchIndexManager.getSearchHistory(startDate, endDate, userId);
    }

    private int calculateUniqueQueries(List<SearchHistory> history) {
        return (int) history.stream()
                .map(SearchHistory::getQuery)
                .distinct()
                .count();
    }

    private double calculateAverageResults(List<SearchHistory> history) {
        return history.stream()
                .mapToInt(SearchHistory::getResultCount)
                .average()
                .orElse(0.0);
    }

    private long countZeroResultSearches(List<SearchHistory> history) {
        return history.stream()
                .filter(SearchHistory::isZeroResults)
                .count();
    }

    private Map<String, Integer> calculatePopularQueries(List<SearchHistory> history, int limit) {
        return history.stream()
                .collect(Collectors.groupingBy(
                        SearchHistory::getQuery,
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().intValue()
                ));
    }

    private double calculateSuccessRate(List<SearchHistory> history) {
        if (history.isEmpty()) {
            return 0.0;
        }

        long successfulSearches = history.stream()
                .filter(h -> !h.isZeroResults())
                .count();

        return (double) successfulSearches / history.size();
    }

    private Map<String, Integer> calculateTimeDistribution(List<SearchHistory> history) {
        Map<String, Integer> distribution = new HashMap<>();

        for (SearchHistory h : history) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date(h.getTimestamp()));
            int hour = cal.get(Calendar.HOUR_OF_DAY);

            String timeSlot;
            if (hour < 6) {
                timeSlot = "00:00-06:00";
            } else if (hour < 12) {
                timeSlot = "06:00-12:00";
            } else if (hour < 18) {
                timeSlot = "12:00-18:00";
            } else {
                timeSlot = "18:00-24:00";
            }

            distribution.put(timeSlot, distribution.getOrDefault(timeSlot, 0) + 1);
        }

        return distribution;
    }

    // ========== 内部类 ==========

    @Data
    public static class SearchQuery {
        private String originalQuery;
        private String userId;
        private ParsedQuery parsedQuery;
        private List<String> expandedTerms;
        private boolean fullTextSearch;
        private boolean semanticSearch;
        private Map<String, String> fieldFilters;
        private Map<String, Double> fieldWeights;

        public boolean hasFieldFilters() {
            return fieldFilters != null && !fieldFilters.isEmpty();
        }
    }

    @Data
    public static class ParsedQuery {
        private List<String> keywords;
        private List<QueryOperator> operators;
        private Map<String, String> fieldFilters;
        private String queryType;
    }

    public enum QueryOperator {
        AND, OR, NOT
    }

    @Data
    public static class SearchResultItem {
        private String documentId;
        private String title;
        private String description;
        private String category;
        private List<String> tags;
        private Date createdTime;
        private Date updatedTime;
        private double score;
        private String searchType;
        private String snippet;
        private List<String> matchedTerms;
        private Map<String, Object> highlights;
    }

    @Data
    public static class WorkflowFeatureVector {
        private double[] textFeatures;
        private Map<String, Object> structureFeatures;
        private Map<String, Object> metadataFeatures;
    }

    @Data
    public static class WorkflowSimilarity {
        private String workflowId;
        private double score;
        private String similarityType;
        private Map<String, Object> details;
    }

    @Data
    public static class DocumentSimilarity {
        private String documentId;
        private double score;
        private double[] documentVector;
    }

    @Data
    public static class SearchHistory {
        private String searchId;
        private String query;
        private String userId;
        private long timestamp;
        private int resultCount;
        private boolean zeroResults;
        private Map<String, Object> filters;
        private long responseTime;
    }

    @Data
    public static class SearchStatistics {
        private Date periodStart;
        private Date periodEnd;
        private int totalSearches;
        private int uniqueQueries;
        private double averageResultsPerSearch;
        private int zeroResultSearches;
        private double successRate;
        private Map<String, Integer> popularQueries;
        private Map<String, Integer> timeDistribution;
    }

    @Data
    public static class SearchSuggestion {
        private String text;
        private String type; // query, correction, completion
        private double score;
        private Map<String, Object> metadata;
    }
}