package com.datalink.platform.llm.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 大模型润色说明项（前端「润色说明」逐条展示）。
 *
 * <p>type 五类约定：rename（改名）/ chain（链路补全）/ party（参与方补充）/
 * relation（关系修正）/ flow（流程模板补全）；另有 noop（兜底直通）/ error（异常降级）。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefinementItem {

    /** rename / chain / party / relation / flow / noop / error */
    private String type;
    /** 说明文字 */
    private String text;
}
