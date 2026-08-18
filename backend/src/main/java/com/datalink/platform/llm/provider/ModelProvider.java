package com.datalink.platform.llm.provider;

import com.datalink.platform.llm.dto.LlmRefineRequest;
import com.datalink.platform.llm.dto.LlmRefineResult;

/**
 * G4 大模型提供方抽象。
 *
 * <p>实现约定：任何异常（HTTP 错误/超时/输出非法）都必须内部消化并降级返回，
 * 绝不向调用方抛异常；未配置密钥时由 NoopModelProvider 兜底直通。
 */
public interface ModelProvider {

    /** 提供方标识（如 openai-compatible / noop） */
    String name();

    /** 是否可用（已配置密钥且可发起调用） */
    boolean available();

    /** 对引擎草稿进行润色 */
    LlmRefineResult refine(LlmRefineRequest req);
}
