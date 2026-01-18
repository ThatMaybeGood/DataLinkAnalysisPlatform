package com.workflow.platform.exception;

import com.workflow.platform.constants.ErrorCodeConstants;

//工作流异常
public class WorkflowException extends RuntimeException {

	private final int errorCode;

	public WorkflowException(String message) {
		super(message);
		this.errorCode = ErrorCodeConstants.SYNC_CONFLICT;
	}

	public WorkflowException(String message, Exception e) {
		super(message, e);
		this.errorCode = ErrorCodeConstants.SYNC_CONFLICT;
	}

	public int getErrorCode() {
		return errorCode;
	}

	@Override
	public String toString() {
		return "WorkflowException{" +
				"errorCode=" + errorCode +
				", message='" + getMessage() + '\'' +
				'}';
	}
}
