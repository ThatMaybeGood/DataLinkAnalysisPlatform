package com.workflow.platform.exception;

import com.workflow.platform.constants.ErrorCodeConstants;
import lombok.Getter;

import java.util.Map;

@Getter
public class VersionException extends RuntimeException {

    private final int errorCode;
    private final String conflictType;
    private final Map<String, Object> localData;
    private final Map<String, Object> remoteData;
    private final String resolutionSuggestion;

    public VersionException(String message) {

        super(message);
        this.errorCode = ErrorCodeConstants.SYNC_CONFLICT;
        this.conflictType = "general";
        this.localData = null;
        this.remoteData = null;
        this.resolutionSuggestion = null;
    }
    public VersionException(String message,Exception e) {

        super(message);
        this.errorCode = ErrorCodeConstants.SYNC_CONFLICT;
        this.conflictType = "general";
        this.localData = null;
        this.remoteData = null;
        this.resolutionSuggestion = null;
    }
}
