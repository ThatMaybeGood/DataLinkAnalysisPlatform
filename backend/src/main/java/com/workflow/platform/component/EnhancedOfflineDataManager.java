package com.workflow.platform.component;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/10 23:49
 */


import com.workflow.platform.util.DataCompressionUtil;
import com.workflow.platform.util.DataEncryptionUtil;
import com.workflow.platform.model.entity.WorkflowEntity;
import com.workflow.platform.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 增强版离线数据管理器（支持压缩和加密）
 */
@Component
@Slf4j
public class EnhancedOfflineDataManager extends OfflineDataManager {

    @Autowired
    private DataCompressionUtil compressionUtil;

    @Autowired
    private DataEncryptionUtil encryptionUtil;

    @Value("${workflow.offline.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${workflow.offline.compression.level:STANDARD}")
    private String compressionLevel;

    @Value("${workflow.offline.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @Value("${workflow.offline.encryption.password:offline_workflow_secure}")
    private String encryptionPassword;

    @Value("${workflow.offline.batch.size:100}")
    private int batchSize;

    /**
     * 保存工作流（增强版：压缩+加密）
     */
     public boolean saveWorkflow(WorkflowEntity workflow) {
        try {
            // 转换为JSON
            String workflowJson = JsonUtil.toJson(workflow);

            // 安全处理数据
            byte[] securedData = secureData(workflowJson);

            // 保存到文件
            String fileName = generateSecureFileName(workflow.getId().toString(), "workflow");
            Path filePath = Path.of(getWorkflowFilePath(fileName));

            Files.write(filePath, securedData);

            // 更新文件索引
            updateFileIndex(fileName, workflow.getId().toString(), "workflow",
                    securedData.length, true);

            log.info("增强版保存工作流成功，ID: {}，文件: {}，大小: {} bytes",
                    workflow.getId(), fileName, securedData.length);
            return true;

        } catch (Exception e) {
            log.error("增强版保存工作流失败，ID: {}", workflow.getId(), e);
            return false;
        }
    }

    /**
     * 批量保存工作流（提高效率）
     */
    public boolean saveWorkflowsInBatch(List<WorkflowEntity> workflows) {
        if (workflows == null || workflows.isEmpty()) {
            return true;
        }

        log.info("开始批量保存工作流，数量: {}", workflows.size());

        try {
            List<String> workflowJsons = new ArrayList<>();
            Map<String, String> idToJsonMap = new HashMap<>();

            // 准备数据
            for (WorkflowEntity workflow : workflows) {
                String workflowJson = JsonUtil.toJson(workflow);
                workflowJsons.add(workflowJson);
                idToJsonMap.put(workflow.getId().toString(), workflowJson);
            }

            // 批量处理
            Map<String, byte[]> processedData = processDataInBatch(workflowJsons);

            // 保存文件
            for (Map.Entry<String, String> entry : idToJsonMap.entrySet()) {
                String workflowId = entry.getKey();
                String workflowJson = entry.getValue();
                byte[] securedData = processedData.get(workflowJson);

                if (securedData != null) {
                    String fileName = generateSecureFileName(workflowId, "workflow");
                    Path filePath = Path.of(getWorkflowFilePath(fileName));

                    Files.write(filePath, securedData);

                    updateFileIndex(fileName, workflowId, "workflow",
                            securedData.length, true);
                }
            }

            log.info("批量保存工作流成功，数量: {}", workflows.size());
            return true;

        } catch (Exception e) {
            log.error("批量保存工作流失败", e);
            return false;
        }
    }

    private void updateFileIndex(String fileName, String workflowId, String workflow, int length, boolean b) {
    }

    /**
     * 读取工作流（增强版：解密+解压）
     */
//    @Override
    public WorkflowEntity loadWorkflow_(String workflowId) {
        try {
            // 从文件索引查找文件
            String fileName = findFileNameById(workflowId, "workflow");
            if (fileName == null) {
                log.warn("未找到工作流文件，ID: {}", workflowId);
                return null;
            }

            Path filePath = Path.of(getWorkflowFilePath(fileName));
            if (!Files.exists(filePath)) {
                log.warn("工作流文件不存在，路径: {}", filePath);
                return null;
            }

            // 读取文件数据
            byte[] fileData = Files.readAllBytes(filePath);

            // 还原数据
            String workflowJson = restoreData(fileData);

            // 解析为对象
            WorkflowEntity workflow = JsonUtil.fromJson(workflowJson, WorkflowEntity.class);

            log.debug("增强版加载工作流成功，ID: {}，文件大小: {} bytes",
                    workflowId, fileData.length);
            return workflow;

        } catch (Exception e) {
            log.error("增强版加载工作流失败，ID: {}", workflowId, e);
            return null;
        }
    }

    private String findFileNameById(String workflowId, String workflow) {
        return null;
    }

    /**
     * 安全处理数据（压缩+加密）
     */
    private byte[] secureData(String data) throws Exception {
        byte[] result = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // 压缩
        if (compressionEnabled) {
            DataCompressionUtil.CompressionLevel level =
                    DataCompressionUtil.CompressionLevel.valueOf(compressionLevel);
            DataCompressionUtil.CompressionResult compressionResult =
                    compressionUtil.smartCompress(data, level);

            if (compressionResult.isCompressed()) {
                result = compressionResult.getCompressedData();
                log.debug("数据压缩完成，压缩率: {:.2f}%",
                        compressionResult.getCompressionRatio() * 100);
            }
        }

        // 加密
        if (encryptionEnabled) {
            DataEncryptionUtil.EncryptionResult encryptionResult =
                    encryptionUtil.encryptWithAES(new String(result), encryptionPassword);

            // 构建包含元数据的文件格式
            result = buildSecureFileFormat(encryptionResult);
        }

        return result;
    }

    /**
     * 还原数据（解密+解压）
     */
    private String restoreData(byte[] securedData) throws Exception {
        byte[] data = securedData;

        // 检查是否需要解密
        if (isEncryptedData(securedData)) {
            DataEncryptionUtil.EncryptionResult encryptionResult =
                    parseSecureFileFormat(securedData);

            String decrypted = encryptionUtil.decryptWithAES(encryptionResult, encryptionPassword);
            data = decrypted.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        // 检查是否需要解压
        DataCompressionUtil.CompressionAlgorithm algorithm =
                compressionUtil.detectCompressionAlgorithm(data);

        if (algorithm != DataCompressionUtil.CompressionAlgorithm.NONE) {
            String decompressed = compressionUtil.decompress(data, algorithm);
            return decompressed;
        }

        return new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * 批量处理数据
     */
    private Map<String, byte[]> processDataInBatch(List<String> dataList) throws Exception {
        Map<String, byte[]> result = new HashMap<>();

        // 分批处理，避免内存溢出
        for (int i = 0; i < dataList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, dataList.size());
            List<String> batch = dataList.subList(i, end);

            for (String data : batch) {
                byte[] securedData = secureData(data);
                result.put(data, securedData);
            }

            log.debug("批量处理数据进度: {}/{}", end, dataList.size());
        }

        return result;
    }

    /**
     * 构建安全文件格式（包含元数据）
     */
    private byte[] buildSecureFileFormat(DataEncryptionUtil.EncryptionResult encryptionResult)
            throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);

        // 文件头：魔数+版本
        dos.writeBytes("WFSEC"); // 魔数
        dos.writeByte(1);       // 版本

        // 加密算法标识
        dos.writeByte(encryptionResult.getAlgorithm().ordinal());

        // 写入IV
        dos.writeInt(encryptionResult.getIv().length);
        dos.write(encryptionResult.getIv());

        // 写入盐
        dos.writeInt(encryptionResult.getSalt().length);
        dos.write(encryptionResult.getSalt());

        // 写入原始大小
        dos.writeInt(encryptionResult.getOriginalSize());

        // 写入加密数据
        dos.writeInt(encryptionResult.getEncryptedData().length);
        dos.write(encryptionResult.getEncryptedData());

        return bos.toByteArray();
    }

    /**
     * 解析安全文件格式
     */
    private DataEncryptionUtil.EncryptionResult parseSecureFileFormat(byte[] fileData)
            throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(fileData);
        DataInputStream dis = new DataInputStream(bis);

        // 读取文件头
        byte[] magic = new byte[5];
        dis.readFully(magic);
        if (!new String(magic).equals("WFSEC")) {
            throw new IOException("无效的安全文件格式");
        }

        byte version = dis.readByte();
        if (version != 1) {
            throw new IOException("不支持的文件版本: " + version);
        }

        // 读取加密算法
        byte algorithmOrdinal = dis.readByte();
        DataEncryptionUtil.EncryptionAlgorithm algorithm =
                DataEncryptionUtil.EncryptionAlgorithm.values()[algorithmOrdinal];

        // 读取IV
        int ivLength = dis.readInt();
        byte[] iv = new byte[ivLength];
        dis.readFully(iv);

        // 读取盐
        int saltLength = dis.readInt();
        byte[] salt = new byte[saltLength];
        dis.readFully(salt);

        // 读取原始大小
        int originalSize = dis.readInt();

        // 读取加密数据
        int dataLength = dis.readInt();
        byte[] encryptedData = new byte[dataLength];
        dis.readFully(encryptedData);

        return new DataEncryptionUtil.EncryptionResult(
                encryptedData, iv, salt, algorithm, originalSize
        );
    }

    /**
     * 检查数据是否加密
     */
    private boolean isEncryptedData(byte[] data) {
        if (data.length < 6) { // 至少需要魔数+版本
            return false;
        }

        try {
            return new String(data, 0, 5).equals("WFSEC");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成安全文件名
     */
    private String generateSecureFileName(String id, String type) {
        String timestamp = LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        );
        String hash = generateContentHash(id + timestamp);
        return String.format("%s_%s_%s.wfsec", type, hash.substring(0, 8), timestamp);
    }

    /**
     * 生成内容哈希
     */
    private String generateContentHash(String content) {
        try {
            return encryptionUtil.generateFileHash(
                    content.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    DataEncryptionUtil.HashAlgorithm.SHA_256
            );
        } catch (Exception e) {
            // 如果哈希生成失败，使用简单哈希
            return Integer.toHexString(content.hashCode());
        }
    }

    /**
     * 备份离线数据（压缩备份）
     */
    public String backupOfflineData(String backupName) throws IOException {
        Path backupDir = Paths.get(getOfflineBasePath(), "backups");
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }

        String backupFileName = String.format("backup_%s_%s.zip",
                backupName,
                LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        );

        Path backupPath = backupDir.resolve(backupFileName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupPath.toFile()))) {
            // 备份工作流文件
            backupDirectory(zos, Paths.get(getOfflineBasePath(), "workflows"), "workflows");

            // 备份节点文件
            backupDirectory(zos, Paths.get(getOfflineBasePath(), "nodes"), "nodes");

            // 备份验证规则文件
            backupDirectory(zos, Paths.get(getOfflineBasePath(), "rules"), "rules");

            // 备份文件索引
            Path indexFile = Paths.get(getOfflineBasePath(), "file-index.json");
            if (Files.exists(indexFile)) {
                addFileToZip(zos, indexFile, "file-index.json");
            }

            // 备份同步状态
            Path syncStateFile = Paths.get(getOfflineBasePath(), "sync", "sync-states.json");
            if (Files.exists(syncStateFile)) {
                addFileToZip(zos, syncStateFile, "sync/sync-states.json");
            }
        }

        log.info("离线数据备份完成，文件: {}，大小: {} bytes",
                backupFileName, Files.size(backupPath));

        return backupPath.toString();
    }

    private String getOfflineBasePath() {
        return null;
    }

    /**
     * 恢复离线数据
     */
    public boolean restoreOfflineData(String backupFilePath) throws IOException {
        Path backupPath = Paths.get(backupFilePath);
        if (!Files.exists(backupPath)) {
            throw new FileNotFoundException("备份文件不存在: " + backupFilePath);
        }

        // 清空现有数据
        clearOfflineData();

        // 恢复数据
        try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                new FileInputStream(backupPath.toFile()))) {

            java.util.zip.ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = Paths.get(getOfflineBasePath(), entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }

                zis.closeEntry();
            }
        }

        log.info("离线数据恢复完成，来源: {}", backupFilePath);
        return true;
    }

    /**
     * 清空离线数据
     */
    private void clearOfflineData() throws IOException {
        Path workflowsDir = Paths.get(getOfflineBasePath(), "workflows");
        Path nodesDir = Paths.get(getOfflineBasePath(), "nodes");
        Path rulesDir = Paths.get(getOfflineBasePath(), "rules");

        deleteDirectory(workflowsDir);
        deleteDirectory(nodesDir);
        deleteDirectory(rulesDir);

        // 重新创建目录
        Files.createDirectories(workflowsDir);
        Files.createDirectories(nodesDir);
        Files.createDirectories(rulesDir);
    }

    /**
     * 备份目录到ZIP
     */
    private void backupDirectory(ZipOutputStream zos, Path dir, String baseName) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }

        Files.walk(dir).forEach(path -> {
            try {
                if (Files.isRegularFile(path)) {
                    Path relativePath = dir.relativize(path);
                    String zipEntryName = baseName + "/" + relativePath.toString();
                    addFileToZip(zos, path, zipEntryName);
                }
            } catch (IOException e) {
                log.error("备份文件失败: {}", path, e);
            }
        });
    }

    /**
     * 添加文件到ZIP
     */
    private void addFileToZip(ZipOutputStream zos, Path file, String entryName) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);
        Files.copy(file, zos);
        zos.closeEntry();
    }

    /**
     * 删除目录
     */
    private void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}