package com.datalink.platform.engine.service.impl;

import com.datalink.platform.common.enums.ResultCode;
import com.datalink.platform.common.exception.BusinessException;
import com.datalink.platform.datasource.ConnectorDbType;
import com.datalink.platform.datasource.dialect.DbDialect;
import com.datalink.platform.datasource.dialect.DbDialectFactory;
import com.datalink.platform.datasource.entity.Connector;
import com.datalink.platform.datasource.mapper.ConnectorMapper;
import com.datalink.platform.datasource.pool.ConnectionPoolRegistry;
import com.datalink.platform.engine.dto.EngineCandidateVO;
import com.datalink.platform.engine.dto.EngineDraftVO;
import com.datalink.platform.engine.dto.EngineFlowVO;
import com.datalink.platform.engine.dto.RefineResultVO;
import com.datalink.platform.engine.service.EngineAnalyzeService;
import com.datalink.platform.llm.dto.LlmRefineRequest;
import com.datalink.platform.llm.dto.LlmRefineResult;
import com.datalink.platform.llm.dto.RefinementItem;
import com.datalink.platform.llm.provider.ModelProvider;
import com.datalink.platform.model.dto.EdgeVO;
import com.datalink.platform.model.dto.NodeVO;
import com.datalink.platform.model.entity.PatternLibrary;
import com.datalink.platform.model.service.PatternLibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 引擎分析实现：通用业务单据模式库识别（文档书 15.3）。
 *
 * <p>扫描流程：复用连接器连接池 → DatabaseMetaData 取表/列/主键 → 采样少量行识别
 * 业务编码号/状态/时间列 → 五大信号打分 → 单号引用链跨表关联（编码号族归属判定）
 * → 产出候选清单（主表识别置信度落设计区间 60~85）+ 草稿节点/边 + 流程模板。
 *
 * <p>信号与权重（实测回填）：
 * <ol>
 *   <li>主键 30：单列主键且样本 id 为纯数字（流水型主单）</li>
 *   <li>业务编码号 18：列名含 no/code 且样本含非纯数字前缀（REA2026...）</li>
 *   <li>状态 16：列名含 status/state，样本值 ≥2 且全短文本（值随流程变化）</li>
 *   <li>时间 10：列名含 time/date</li>
 *   <li>主子表 8：存在 `x_id` 外键列（明细行挂主单）</li>
 *   <li>引用：被引用 refIn×3 上限 9 + 引用他表 refOut×2 上限 4（单号引用链）</li>
 * </ol>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EngineAnalyzeServiceImpl implements EngineAnalyzeService {

    private static final int SAMPLE_LIMIT = 40;
    private static final int LOW_CONFIDENCE = 70;
    private static final int MAX_FLOWS = 20;
    private static final int MAX_FLOW_DEPTH = 6;

    private final ConnectorMapper connectorMapper;
    private final ConnectionPoolRegistry poolRegistry;
    private final ModelProvider modelProvider;
    private final PatternLibraryService patternLibraryService;

    /** 单列元信息 */
    private static class ColMeta {
        String name;
        String type;
        boolean nullable;
        boolean autoIncrement;
        boolean pk;
    }

    /** 表对象 */
    private static class TableMeta {
        String name;
        String type;
        String comment = "";
        List<ColMeta> columns = new ArrayList<>();
        List<Long> pkIds = new ArrayList<>();
        List<String> statusValues = new ArrayList<>();
        // 编码列采样：列名 → 去重样本
        Map<String, Set<String>> codeSample = new HashMap<>();
        Set<String> refOut = new HashSet<>();   // 消费了哪些表（provider）
        Set<String> refIn = new HashSet<>();    // 被哪些表消费
        boolean sub;
        int score;
        List<String> marks = new ArrayList<>();
        List<String> nodeIds = new ArrayList<>(); // 流程链上的节点 id（多成员共享，取首个去重）
    }

    @Override
    public EngineDraftVO analyze(Long connectorId) {
        Connector c = connectorMapper.selectById(connectorId);
        if (c == null || c.getConnectorType() == null || !"DB".equalsIgnoreCase(c.getConnectorType())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "DB 连接不存在: " + connectorId);
        }
        ConnectorDbType.from(c.getDbType()); // 校验方言
        if (c.getEnabled() == null || c.getEnabled() != 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "连接未启用，请先在数据池启用");
        }

        DbDialect dialect = DbDialectFactory.ofCode(c.getDbType());
        DataSource ds = poolRegistry.get(c);
        String db = c.getDatabaseName();

        List<TableMeta> tables = new ArrayList<>();
        try (Connection conn = ds.getConnection()) {
            conn.setReadOnly(true);
            DatabaseMetaData md = conn.getMetaData();
            String schema = (c.getSchemaName() == null || c.getSchemaName().isBlank())
                    ? null : c.getSchemaName();
            try (ResultSet rs = md.getTables(null, schema, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String name = rs.getString("TABLE_NAME");
                    if (name == null || name.isEmpty()) continue;
                    TableMeta t = new TableMeta();
                    t.name = name;
                    t.type = rs.getString("TABLE_TYPE");
                    tables.add(t);
                }
            }
            loadComments(conn, tables);

            for (TableMeta t : tables) {
                try (ResultSet rs = md.getColumns(null, schema, t.name, "%")) {
                    while (rs.next()) {
                        ColMeta col = new ColMeta();
                        col.name = rs.getString("COLUMN_NAME");
                        col.type = rs.getString("TYPE_NAME");
                        int nullable = rs.getInt("NULLABLE");
                        col.nullable = nullable == DatabaseMetaData.columnNullable;
                        col.autoIncrement = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                        t.columns.add(col);
                    }
                }
                try (ResultSet rs = md.getPrimaryKeys(null, schema, t.name)) {
                    while (rs.next()) {
                        int seq = rs.getInt("KEY_SEQ");
                        String col = rs.getString("COLUMN_NAME");
                        if (seq == 1) {
                            for (ColMeta cm : t.columns) {
                                if (cm.name.equalsIgnoreCase(col)) cm.pk = true;
                            }
                        }
                    }
                }
                sample(t, conn, dialect);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BAD_REQUEST.getCode(), "分析失败: " + shortMessage(e));
        }

        // 排除元数据表（如 flyway_schema_history 等），后续识别/链接/流程均只处理业务表
        List<TableMeta> businessTables = tables.stream()
                .filter(t -> !META_TABLES.contains(t.name.toLowerCase(Locale.ROOT)))
                .toList();
        for (TableMeta t : businessTables) {
            recognize(t);
        }
        linkReferences(businessTables);
        buildFlows(businessTables);
        List<EngineCandidateVO> candidates = new ArrayList<>();
        for (TableMeta t : businessTables) {
            if (t.sub || t.score > 0) {
                candidates.add(toCandidate(t));
            }
        }
        candidates.sort(Comparator.comparingInt(EngineCandidateVO::getConfidence).reversed());

        // G5：对候选名称应用模式库自动校正（命中模式则提升置信度并改名）
        applyPatternCorrections(candidates);

        EngineDraftVO draft = new EngineDraftVO();
        draft.setDatabase(db);
        draft.setCandidates(candidates);
        draft.setMessage("扫描 " + tables.size() + " 张表，命中 " + candidates.size() + " 个候选单据信号");

        // 流程模板（依据 refOut/refIn 数据方向去重拼装）
        LinkedHashSet<String> emitted = new LinkedHashSet<>();
        for (TableMeta t : businessTables) {
            List<String> chain = t.nodeIds;
            if (chain.isEmpty() || !emitted.add(chain.get(0))) continue;
            List<String> ids = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<String> tb = new ArrayList<>();
            boolean ok = chain.stream().allMatch(id -> {
                TableMeta tm = idTableMeta(tables, id);
                if (tm == null) return false;
                if (META_TABLES.contains(tm.name.toLowerCase(Locale.ROOT))) return false;
                ids.add(id);
                names.add(tm.comment == null || tm.comment.isBlank() ? tm.name : tm.comment);
                tb.add(tm.name);
                return true;
            });
            if (ok && names.size() >= 2) {
                EngineFlowVO flow = new EngineFlowVO();
                flow.setName(String.join("→", names));
                flow.setNodeIds(ids);
                flow.setTableNames(tb);
                draft.getFlows().add(flow);
            }
        }
        if (draft.getFlows().size() > MAX_FLOWS) {
            draft.setFlows(new ArrayList<>(draft.getFlows().subList(0, MAX_FLOWS)));
        }

        buildGraph(draft, businessTables, candidates, db);
        return draft;
    }

    /**
     * G4 大模型细化：引擎骨架 → ModelProvider 增量细化。
     *
     * <p>降级策略：任何来自 modelProvider 的异常不向上抛——记录日志后返回
     * refinements=[("error", ...)]、provider="error" 的结果，base（引擎原稿）仍返回，
     * 前端可回退到 G2/G3 纯引擎视图。
     */
    @Override
    public RefineResultVO refine(Long connectorId) {
        EngineDraftVO base = analyze(connectorId);

        LlmRefineRequest req = LlmRefineRequest.builder()
                .database(base.getDatabase())
                .candidates(base.getCandidates())
                .flows(base.getFlows())
                .build();

        LlmRefineResult r;
        try {
            r = modelProvider.refine(req);
        } catch (Exception e) {
            log.error("大模型细化调用异常，降级返回引擎原稿: {}", shortMessage(e), e);
            return RefineResultVO.builder()
                    .base(base)
                    .addedNodes(Collections.emptyList())
                    .addedEdges(Collections.emptyList())
                    .renameMap(Collections.emptyMap())
                    .refinements(List.of(new RefinementItem("error", "大模型调用异常，已返回引擎原稿")))
                    .provider("error")
                    .message("大模型调用异常，已返回引擎原稿")
                    .build();
        }

        if (r == null) {
            r = LlmRefineResult.builder().build();
        }
        return RefineResultVO.builder()
                .base(base)
                .addedNodes(r.getAddedNodes() == null ? Collections.emptyList() : r.getAddedNodes())
                .addedEdges(r.getAddedEdges() == null ? Collections.emptyList() : r.getAddedEdges())
                .renameMap(r.getRenameMap() == null ? Collections.emptyMap() : r.getRenameMap())
                .refinements(r.getRefinements() == null ? Collections.emptyList() : r.getRefinements())
                .provider(r.getProvider())
                .message(r.getMessage())
                .build();
    }

    /** nodeId → TableMeta */
    private TableMeta idTableMeta(List<TableMeta> tables, String id) {
        if (!id.startsWith("t-")) return null;
        String name = id.substring(2);
        for (TableMeta t : tables) {
            if (t.name.equalsIgnoreCase(name)) return t;
        }
        return null;
    }

    /** 尝试取表注释（兼容 MySQL / H2） */
    private void loadComments(Connection conn, List<TableMeta> tables) {
        Map<String, TableMeta> byName = new HashMap<>();
        for (TableMeta t : tables) byName.put(t.name.toLowerCase(Locale.ROOT), t);
        String lower = "";
        try {
            lower = conn.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        } catch (Exception ignore) {
            return;
        }
        String sql;
        if (lower.contains("mysql")) {
            sql = "SELECT TABLE_NAME, TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES";
        } else if (lower.contains("h2")) {
            sql = "SELECT TABLE_NAME, REMARKS FROM INFORMATION_SCHEMA.TABLES";
        } else {
            return;
        }
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                TableMeta t = byName.get(rs.getString(1).toLowerCase(Locale.ROOT));
                if (t != null && rs.getString(2) != null) t.comment = rs.getString(2);
            }
        } catch (Exception ignore) {
            // 权限不足时跳过注释
        }
    }

    /** 采样少量行 */
    private void sample(TableMeta t, Connection conn, DbDialect dialect) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT * FROM " + dialect.quote(t.name) + " LIMIT " + SAMPLE_LIMIT)) {
            ResultSetMetaData rsm = rs.getMetaData();
            int n = rsm.getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= n; i++) {
                    String col = rsm.getColumnLabel(i).toLowerCase(Locale.ROOT);
                    Object v = rs.getObject(i);
                    String sv = v == null ? null : String.valueOf(v).trim();
                    if (sv == null || sv.isEmpty()) continue;
                    if ("id".equals(col)) {
                        if (isNumeric(sv)) t.pkIds.add(extractLong(sv));
                    } else if (isCodeCol(col)) {
                        t.codeSample.computeIfAbsent(col, k -> new HashSet<>()).add(sv);
                    } else if (isStatusCol(col)) {
                        if (t.statusValues.size() < 12) t.statusValues.add(sv);
                    }
                }
            }
        } catch (Exception ignore) {
            // 无法采样按无数据识别
        }
    }

    /** 主信号打分 */
    private void recognize(TableMeta t) {
        int score = 0;
        List<ColMeta> pks = t.columns.stream().filter(c -> c.pk).toList();
        // ① 主键：单列主键且样本纯数字（流水型主单）
        if (pks.size() == 1 && !t.pkIds.isEmpty()) {
            score += 30;
            t.marks.add("主键");
        } else if (pks.size() == 1) {
            score += 12; // 有主键但无样本（空表）→ 弱
            t.marks.add("主键");
        }
        // ② 业务编码号
        boolean codeCol = t.columns.stream().anyMatch(c -> isCodeCol(c.name));
        boolean codeVal = t.codeSample.values().stream()
                .flatMap(Set::stream).anyMatch(v -> !isNumeric(v) && v.length() >= 6);
        if (codeCol && codeVal) {
            score += 18;
            t.marks.add("单号");
        } else if (codeCol) {
            score += 6;
            t.marks.add("单号");
        }
        // ③ 状态字段（值随流程变化）
        if (isStatusField(t.statusValues)) {
            score += 16;
            t.marks.add("状态");
        }
        // ④ 时间字段
        boolean hasTime = t.columns.stream().anyMatch(c -> isTimeCol(c.name));
        if (hasTime) {
            score += 10;
            t.marks.add("时间");
        }
        // ⑤ 主子表：存在 x_id 外键列
        boolean hasFk = t.columns.stream().anyMatch(c -> isFkCol(c.name) && !c.pk);
        if (hasFk) {
            t.sub = true;
            score += 8;
            t.marks.add("主子表");
        }
        t.score = Math.min(score, 100);
    }

    /** 单号引用链：编码号族归属判定 */
    private void linkReferences(List<TableMeta> tables) {
        for (TableMeta t : tables) {
            for (ColMeta c : t.columns) {
                String ln = c.name.toLowerCase(Locale.ROOT);
                // 业务编码号列：同列名家族找唯一拥有者
                if (isCodeCol(ln) && !c.pk) {
                    boolean tHasOtherCode = t.columns.stream()
                            .anyMatch(o -> isCodeCol(o.name) && !o.name.equalsIgnoreCase(ln));
                    if (!tHasOtherCode) continue; // 本表唯一编码号 = 自身身份，非引用
                    TableMeta owner = codeOwner(tables, t, ln);
                    if (owner != null && owner != t) {
                        t.refOut.add(owner.name.toLowerCase(Locale.ROOT));
                        owner.refIn.add(t.name.toLowerCase(Locale.ROOT));
                    }
                }
                // 外键列 x_id → 主表主键列（表名前缀）
                if (isFkCol(ln) && !c.pk) {
                    for (TableMeta other : tables) {
                        if (other == t) continue;
                        boolean pkName = other.columns.stream()
                                .anyMatch(oc -> oc.pk && oc.name.equalsIgnoreCase(ln));
                        if (pkName) {
                            t.refOut.add(other.name.toLowerCase(Locale.ROOT));
                            other.refIn.add(t.name.toLowerCase(Locale.ROOT));
                        } else {
                            String prefix = ln.substring(0, ln.length() - 3); // 去掉 _id
                            String oname = other.name.toLowerCase(Locale.ROOT);
                            if (oname.equals(prefix) || oname.startsWith(prefix + "_")) {
                                t.refOut.add(oname);
                                other.refIn.add(t.name.toLowerCase(Locale.ROOT));
                            }
                        }
                    }
                }
            }
        }
        // 引用加分（被引用越多越靠主干；上限 9+4）
        for (TableMeta t : tables) {
            int refScore = Math.min(t.refIn.size() * 3, 9) + Math.min(t.refOut.size() * 2, 4);
            if (refScore > 0) {
                t.score = Math.min(t.score + refScore, 100);
                t.marks.add("引用");
            }
        }
    }

    /** 依据引用方向构建流程链（数据方向 provider→consumer） */
    private void buildFlows(List<TableMeta> tables) {
        List<TableMeta> sources = tables.stream()
                .filter(t -> t.refOut.isEmpty() && !t.refIn.isEmpty())
                .sorted(Comparator.comparingInt((TableMeta t) -> t.refIn.size()).thenComparing(t -> t.name))
                .toList();
        for (TableMeta start : sources) {
            List<TableMeta> chain = new ArrayList<>();
            TableMeta cur = start;
            chain.add(cur);
            int guard = 0;
            while (guard++ < MAX_FLOW_DEPTH) {
                TableMeta next = null;
                for (TableMeta cand : tables) {
                    if (cand != cur && cand.refOut.contains(cur.name.toLowerCase(Locale.ROOT))
                            && !chain.contains(cand)) {
                        next = cand;
                        break;
                    }
                }
                if (next == null) break;
                chain.add(next);
                cur = next;
            }
            if (chain.size() >= 2) {
                List<String> ids = chain.stream().map(x -> "t-" + x.name).toList();
                for (TableMeta tm : chain) tm.nodeIds = new ArrayList<>(ids);
            }
        }
    }

    /**
     * 编码号族拥有者判定（单号引用链方向）：
     * ① 前缀匹配优先：`fee_no` → 前缀 `fee` → 表名 `fee_*`（如 fee_order）＝编码号签发方
     * ② 无前缀表名 → 含该列且编码列数最少者（身份最单一者更可能是拥有者），再样本多样性、再名称
     */
    private TableMeta codeOwner(List<TableMeta> tables, TableMeta self, String col) {
        String ln = col.toLowerCase(Locale.ROOT);
        String prefix = ln.endsWith("_no")
                ? ln.substring(0, ln.length() - 3)
                : ln.endsWith("_code") ? ln.substring(0, ln.length() - 5) : ln;
        List<TableMeta> fam = new ArrayList<>();
        List<TableMeta> pref = new ArrayList<>();
        for (TableMeta o : tables) {
            if (o == self) continue;
            boolean has = o.columns.stream().anyMatch(c -> c.name.equalsIgnoreCase(ln));
            if (!has) continue;
            fam.add(o);
            String oname = o.name.toLowerCase(Locale.ROOT);
            if (oname.equals(prefix) || oname.startsWith(prefix + "_")) pref.add(o);
        }
        if (fam.isEmpty()) return null;
        if (pref.size() == 1) return pref.get(0);
        if (pref.size() > 1) {
            return pref.stream().min(Comparator
                    .comparingLong((TableMeta o) -> o.columns.stream().filter(c -> isCodeCol(c.name)).count())
                    .thenComparing(o -> o.name)).get();
        }
        // 无前缀匹配：编码列数最少 → 样本多样性 → 名称（确定性）
        return fam.stream().min(Comparator
                        .comparingLong((TableMeta o) -> o.columns.stream().filter(c -> isCodeCol(c.name)).count())
                        .thenComparingInt((TableMeta o) -> distinct(o, ln))
                        .thenComparing(o -> o.name))
                .get();
    }

    private int distinct(TableMeta t, String col) {
        Set<String> s = t.codeSample.get(col.toLowerCase(Locale.ROOT));
        return s == null ? 0 : s.size();
    }

    /** 候选 VO */
    private EngineCandidateVO toCandidate(TableMeta t) {
        EngineCandidateVO vo = new EngineCandidateVO();
        String comment = t.comment == null ? "" : t.comment.trim();
        vo.setName(comment.isEmpty() ? t.name : comment);
        vo.setTable(t.name);
        int conf = Math.round(t.score / 5f) * 5;
        vo.setConfidence(Math.min(Math.max(conf, 5), 100));
        vo.getMarks().addAll(t.marks);
        boolean fmt = t.codeSample.values().stream()
                .flatMap(Set::stream).anyMatch(v -> !isNumeric(v) && v.length() >= 6);
        if (fmt) vo.getMarks().add("单号格式");
        vo.setLow(vo.getConfidence() < LOW_CONFIDENCE);
        return vo;
    }

    /** G5：模式库自动应用——命中历史校正/确认的名称则自动改名并提升置信度 */
    private void applyPatternCorrections(List<EngineCandidateVO> candidates) {
        if (patternLibraryService == null) {
            return;
        }
        for (EngineCandidateVO c : candidates) {
            List<PatternLibrary> hits = patternLibraryService.findPatterns("NODE_NAME", c.getName());
            if (hits == null || hits.isEmpty()) {
                continue;
            }
            PatternLibrary p = hits.get(0);
            if (p.getPatternValue() != null && !p.getPatternValue().isBlank()
                    && !p.getPatternValue().equals(c.getName())) {
                c.setName(p.getPatternValue());
                c.getMarks().add("模式校正");
                c.setConfidence(Math.min(c.getConfidence() + 5, 100));
                c.setLow(c.getConfidence() < LOW_CONFIDENCE);
            }
            patternLibraryService.hit(p);
        }
    }

    /** 排除元数据/内部表（不会当成业务表）。
 * H2 多连接时会创建 session 级辅助表（locks/query_statistics/sessions/session_state），
 * 需显式跳过，否则会被当成候选单据。 */
    private static final Set<String> META_TABLES = new HashSet<>(Set.of(
            "flyway_schema_history",
            "information_schema.tables",
            "information_schema.columns",
            "information_schema.statistics",
            "schema_privileges",
            "locks",
            "query_statistics",
            "sessions",
            "session_state"
    ));

    /** 草稿节点/边：库→表 承载边 + 引用方向边 */
    private void buildGraph(EngineDraftVO draft, List<TableMeta> tables,
                            List<EngineCandidateVO> candidates, String db) {
        NodeVO dbNode = new NodeVO();
        dbNode.setId("db-" + db);
        dbNode.setName(db);
        dbNode.setCode(db);
        dbNode.setNodeType("DATABASE");
        dbNode.setLevel("L1");
        dbNode.setStatus("ACTIVE");
        dbNode.setDescription("扫描库 " + db);
        draft.getDraftNodes().add(dbNode);

        Map<String, String> tableId = new LinkedHashMap<>();
        for (EngineCandidateVO c : candidates) {
            String nid = "t-" + c.getTable();
            tableId.put(c.getTable().toLowerCase(Locale.ROOT), nid);
            NodeVO n = new NodeVO();
            n.setId(nid);
            n.setName(c.getName());
            n.setCode(c.getTable());
            n.setNodeType("TABLE");
            n.setLevel(c.getConfidence() >= 70 ? "L2" : "L3");
            n.setStatus("ACTIVE");
            n.setDescription("候选单据 置信度 " + c.getConfidence() + "%");
            draft.getDraftNodes().add(n);
        }

        int edgeIdx = 0;
        for (String tid : tableId.values()) {
            EdgeVO e = new EdgeVO();
            e.setId("e" + (edgeIdx++));
            e.setSource(dbNode.getId());
            e.setTarget(tid);
            e.setRelationType("DATA_FLOW");
            draft.getDraftEdges().add(e);
        }
        // 引用方向边：data provider → consumer
        for (TableMeta t : tables) {
            String consumer = tableId.get(t.name.toLowerCase(Locale.ROOT));
            if (consumer == null) continue;
            for (String ref : t.refOut) {
                String provider = tableId.get(ref);
                if (provider == null) continue;
                EdgeVO e = new EdgeVO();
                e.setId("e" + (edgeIdx++));
                e.setSource(provider);
                e.setTarget(consumer);
                e.setRelationType("DATA_FLOW");
                draft.getDraftEdges().add(e);
            }
        }
    }

    // ---------- 列名判定 ----------

    private boolean isCodeCol(String col) {
        return col.endsWith("_no") || col.endsWith("_code") || "no".equals(col) || "code".equals(col);
    }

    private boolean isStatusCol(String col) {
        return col.contains("status") || col.contains("state") || col.endsWith("_st");
    }

    private boolean isTimeCol(String col) {
        return col.endsWith("_time") || col.endsWith("_date")
                || col.endsWith("_ts") || "time".equals(col) || "date".equals(col);
    }

    private boolean isFkCol(String col) {
        return col.endsWith("_id") && !"id".equals(col);
    }

    // ---------- 值形态判定 ----------

    private boolean isNumeric(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        return true;
    }

    private Long extractLong(String s) {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 状态字段：样本值 ≥2 且全为短文本 */
    private boolean isStatusField(List<String> values) {
        if (values.size() < 2) return false;
        for (String v : values) {
            if (v.length() > 8) return false;
        }
        return true;
    }

    private String shortMessage(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isBlank()) msg = e.getClass().getSimpleName();
        return msg.length() > 200 ? msg.substring(0, 200) : msg;
    }
}