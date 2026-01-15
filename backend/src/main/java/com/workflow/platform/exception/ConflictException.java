package com.workflow.platform.exception;

import com.workflow.platform.constants.ErrorCodeConstants;
import com.workflow.platform.model.entity.ConflictRecordEntity;
import lombok.Getter;

import java.util.Map;

/**
 * 冲突异常
 */
@Getter
public class ConflictException extends RuntimeException {

    private final int errorCode;
    private final String conflictType;
    private final Map<String, Object> localData;
    private final Map<String, Object> remoteData;
    private final String resolutionSuggestion;

    public ConflictException(String message) {
        super(message);
        this.errorCode = ErrorCodeConstants.SYNC_CONFLICT;
        this.conflictType = "general";
        this.localData = null;
        this.remoteData = null;
        this.resolutionSuggestion = null;
    }

    public ConflictException(String message, String conflictType) {
        super(message);
        this.errorCode = ErrorCodeConstants.SYNC_CONFLICT;
        this.conflictType = conflictType;
        this.localData = null;
        this.remoteData = null;
        this.resolutionSuggestion = null;
    }

    public ConflictException(String message, ConflictRecordEntity conflictType) {
        super(message);
        this.errorCode = ErrorCodeConstants.SYNC_CONFLICT;
        this.conflictType = String.valueOf(conflictType);
        this.localData = null;
        this.remoteData = null;
        this.resolutionSuggestion = null;
    }

    public ConflictException(String message, String conflictType,
                             Map<String, Object> localData,
                             Map<String, Object> remoteData) {
        super(message);
        this.errorCode = ErrorCodeConstants.SYNC_CONFLICT;
        this.conflictType = conflictType;
        this.localData = localData;
        this.remoteData = remoteData;
        this.resolutionSuggestion = generateSuggestion(conflictType);
    }

    public ConflictException(String message, String conflictType,
                             Map<String, Object> localData,
                             Map<String, Object> remoteData,
                             String resolutionSuggestion) {
        super(message);
        this.errorCode = ErrorCodeConstants.SYNC_CONFLICT;
        this.conflictType = conflictType;
        this.localData = localData;
        this.remoteData = remoteData;
        this.resolutionSuggestion = resolutionSuggestion;
    }

    private String generateSuggestion(String conflictType) {
        switch (conflictType) {
            case "version":
                return "请检查版本号，选择保留较高版本或合并更改";
            case "timestamp":
                return "请检查修改时间，选择保留最新版本";
            case "data":
                return "数据内容冲突，请手动比较并合并";
            case "reference":
                return "引用关系冲突，请检查相关依赖";
            case "permission":
                return "权限设置冲突，请检查用户权限";
            default:
                return "发现数据冲突，请手动解决";
        }
    }

    @Override
    public String toString() {
        return "ConflictException{" +
                "errorCode=" + errorCode +
                ", conflictType='" + conflictType + '\'' +
                ", message='" + getMessage() + '\'' +
                ", suggestion='" + resolutionSuggestion + '\'' +
                '}';
    }
}