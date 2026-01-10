package com.workflow.platform.constants;
//错误码常量

public class ErrorCodeConstants {

    // 系统错误码 (10000-19999)
    public static final int SYSTEM_ERROR = 10000;
    public static final int DATABASE_ERROR = 10001;
    public static final int NETWORK_ERROR = 10002;
    public static final int FILE_SYSTEM_ERROR = 10003;
    public static final int CONFIGURATION_ERROR = 10004;
    public static final int PERMISSION_DENIED = 10005;
    public static final int AUTHENTICATION_FAILED = 10006;
    public static final int RESOURCE_NOT_FOUND = 10007;
    public static final int SERVICE_UNAVAILABLE = 10008;
    public static final int TIMEOUT_ERROR = 10009;
    public static final int VALIDATION_ERROR = 10010;

    // 业务错误码 (20000-29999)
    public static final int WORKFLOW_NOT_FOUND = 20000;
    public static final int WORKFLOW_EXISTS = 20001;
    public static final int WORKFLOW_INVALID = 20002;
    public static final int WORKFLOW_CYCLE_DETECTED = 20003;
    public static final int WORKFLOW_EXECUTION_FAILED = 20004;

    public static final int NODE_NOT_FOUND = 20100;
    public static final int NODE_INVALID = 20101;
    public static final int NODE_CONNECTION_ERROR = 20102;

    public static final int VALIDATION_RULE_NOT_FOUND = 20200;
    public static final int VALIDATION_RULE_INVALID = 20201;
    public static final int VALIDATION_FAILED = 20202;

    public static final int CONNECTOR_NOT_FOUND = 20300;
    public static final int CONNECTOR_INVALID = 20301;

    public static final int EXECUTION_NOT_FOUND = 20400;
    public static final int EXECUTION_IN_PROGRESS = 20401;

    // 模式相关错误码 (30000-39999)
    public static final int MODE_NOT_SUPPORTED = 30000;
    public static final int MODE_SWITCH_FAILED = 30001;
    public static final int OFFLINE_DATA_CORRUPTED = 30002;
    public static final int SYNC_CONFLICT = 30003;
    public static final int SYNC_FAILED = 30004;

    // 文件相关错误码 (40000-49999)
    public static final int FILE_NOT_FOUND = 40000;
    public static final int FILE_READ_ERROR = 40001;
    public static final int FILE_WRITE_ERROR = 40002;
    public static final int FILE_PERMISSION_DENIED = 40003;
    public static final int FILE_FORMAT_INVALID = 40004;
    public static final int FILE_SIZE_EXCEEDED = 40005;

    // 参数错误码 (50000-59999)
    public static final int PARAMETER_REQUIRED = 50000;
    public static final int PARAMETER_INVALID = 50001;
    public static final int PARAMETER_TYPE_MISMATCH = 50002;
    public static final int PARAMETER_OUT_OF_RANGE = 50003;

    private ErrorCodeConstants() {
        throw new IllegalStateException("错误码常量类");
    }
}