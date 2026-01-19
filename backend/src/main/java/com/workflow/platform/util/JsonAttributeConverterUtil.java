package com.workflow.platform.util;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/19 21:41
 */

/**
 * 通用 JSON 属性转换器，调用项目统一的 JsonUtil
 */
@Converter(autoApply = false)
public class JsonAttributeConverterUtil implements AttributeConverter<Object, String> {

    @Override
    public String convertToDatabaseColumn(Object attribute) {
        if (attribute == null) {
            return null;
        }
        // 使用你定义的 JsonUtil 序列化
        return JsonUtil.toJson(attribute);
    }

    @Override
    public Object convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return null;
        }
        // 使用你定义的 JsonUtil 反序列化
        // 注意：这里默认转为 Object (Jackson 通常会解析为 LinkedHashMap)
        return JsonUtil.fromJson(dbData, Object.class);
    }
}