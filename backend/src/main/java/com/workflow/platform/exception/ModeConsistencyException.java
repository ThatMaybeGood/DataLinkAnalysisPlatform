package com.workflow.platform.exception;

import com.workflow.platform.constants.ErrorCodeConstants;

/**
 * 模式一致性异常
 */
public class ModeConsistencyException extends RuntimeException {

    private final int errorCode;
    private final String clientId;
    private final String clientMode;
    private final String serverMode;

    public ModeConsistencyException(String message) {
        super(message);
        this.errorCode = ErrorCodeConstants.MODE_NOT_SUPPORTED;
        this.clientId = null;
        this.clientMode = null;
        this.serverMode = null;
    }

    public ModeConsistencyException(String message, String clientId,
                                    String clientMode, String serverMode) {
        super(message);
        this.errorCode = ErrorCodeConstants.MODE_NOT_SUPPORTED;
        this.clientId = clientId;
        this.clientMode = clientMode;
        this.serverMode = serverMode;
    }

    public ModeConsistencyException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCodeConstants.MODE_NOT_SUPPORTED;
        this.clientId = null;
        this.clientMode = null;
        this.serverMode = null;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientMode() {
        return clientMode;
    }

    public String getServerMode() {
        return serverMode;
    }

    @Override
    public String toString() {
        return String.format(
                "ModeConsistencyException{错误码=%d, 客户端ID=%s, 客户端模式=%s, 服务器模式=%s, 消息='%s'}",
                errorCode, clientId, clientMode, serverMode, getMessage()
        );
    }
}