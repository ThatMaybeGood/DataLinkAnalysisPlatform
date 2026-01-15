package com.workflow.platform.service;

import com.workflow.platform.model.dto.WorkflowExportDataDTO;

public interface FileStorageService {
    void storeFile(String path, byte[] content);
    byte[] readFile(String path);
    void deleteFile(String path);

    WorkflowExportDataDTO importFromFile(String filePath);

    String exportToFile(WorkflowExportDataDTO exportData);
}
