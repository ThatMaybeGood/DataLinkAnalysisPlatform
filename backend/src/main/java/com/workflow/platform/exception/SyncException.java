package com.workflow.platform.exception;
//同步异常

import com.workflow.platform.constants.ErrorCodeConstants;

/**
 * 同步异常
 */
public class SyncException extends RuntimeException {

    private final int errorCode;
    private final String errorDetail;

    public SyncException(String message) {
        super(message);
        this.errorCode = ErrorCodeConstants.SYNC_FAILED;
        this.errorDetail = null;
    }

    public SyncException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCodeConstants.SYNC_FAILED;
        this.errorDetail = null;
    }

    public SyncException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetail = null;
    }

    public SyncException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorDetail = null;
    }

    public SyncException(String message, int errorCode, String errorDetail) {
        super(message);
        this.errorCode = errorCode;
        this.errorDetail = errorDetail;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorDetail() {
        return errorDetail;
    }

    @Override
    public String toString() {
        String base = "SyncException{" +
                "errorCode=" + errorCode +
                ", message='" + getMessage() + '\'';

        if (errorDetail != null) {
            base += ", errorDetail='" + errorDetail + '\'';
        }

        return base + '}';
    }
}
