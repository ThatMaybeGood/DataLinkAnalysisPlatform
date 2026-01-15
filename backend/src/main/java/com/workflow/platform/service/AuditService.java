package com.workflow.platform.service;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/16 00:25
 */
public interface AuditService {
    void recordAudit(String action, String details, String performedBy);
    void log(String action, String details, String performedBy);

    void log(String action, String details);

    void log(String action);

}
