package com.workflow.platform.util;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/10 23:49
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * 数据加密工具类
 */
@Component
@Slf4j
public class DataEncryptionUtil {

    @Value("${workflow.encryption.aes-key:defaultEncryptionKey123}")
    private String defaultAesKey;

    @Value("${workflow.encryption.algorithm:AES/GCM/NoPadding}")
    private String encryptionAlgorithm;

    @Value("${workflow.encryption.key-derivation-algorithm:PBKDF2WithHmacSHA256}")
    private String keyDerivationAlgorithm;

    @Value("${workflow.encryption.salt:workflow_salt_2024}")
    private String salt;

    @Value("${workflow.encryption.iteration-count:65536}")
    private int iterationCount;

    @Value("${workflow.encryption.key-length:256}")
    private int keyLength;

    @Value("${workflow.encryption.gcm-tag-length:128}")
    private int gcmTagLength;

    private static final String AES_ALGORITHM = "AES";
    private static final int IV_LENGTH = 12; // GCM推荐使用12字节的IV
    private static final int SALT_LENGTH = 16;

    /**
     * 使用AES-GCM加密数据
     */
    public EncryptionResult encryptWithAES(String data) throws Exception {
        return encryptWithAES(data, defaultAesKey);
    }

    /**
     * 使用AES-GCM加密数据（指定密钥）
     */
    public EncryptionResult encryptWithAES(String data, String password) throws Exception {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("加密数据不能为空");
        }

        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        // 生成随机盐
        byte[] saltBytes = generateSalt();

        // 从密码和盐派生密钥
        SecretKey key = deriveKey(password, saltBytes);

        // 生成随机IV
        byte[] iv = generateIV();

        // 创建GCMParameterSpec
        GCMParameterSpec gcmSpec = new GCMParameterSpec(gcmTagLength, iv);

        // 初始化Cipher
        Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

        // 执行加密
        byte[] encryptedData = cipher.doFinal(dataBytes);

        // 构建结果
        return new EncryptionResult(
                encryptedData,
                iv,
                saltBytes,
                EncryptionAlgorithm.AES_GCM,
                dataBytes.length
        );
    }

    /**
     * 使用AES-GCM解密数据
     */
    public String decryptWithAES(EncryptionResult encryptionResult) throws Exception {
        return decryptWithAES(encryptionResult, defaultAesKey);
    }

    /**
     * 使用AES-GCM解密数据（指定密钥）
     */
    public String decryptWithAES(EncryptionResult encryptionResult, String password) throws Exception {
        // 从密码和盐派生密钥
        SecretKey key = deriveKey(password, encryptionResult.getSalt());

        // 创建GCMParameterSpec
        GCMParameterSpec gcmSpec = new GCMParameterSpec(gcmTagLength, encryptionResult.getIv());

        // 初始化Cipher
        Cipher cipher = Cipher.getInstance(encryptionAlgorithm);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

        // 执行解密
        byte[] decryptedData = cipher.doFinal(encryptionResult.getEncryptedData());

        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * 使用AES-CBC加密数据（兼容性更好）
     */
    public EncryptionResult encryptWithAESCBC(String data, String password) throws Exception {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] saltBytes = generateSalt();

        // 派生密钥
        SecretKey key = deriveKey(password, saltBytes);

        // 生成随机IV
        byte[] iv = generateIVForCBC();

        // 初始化Cipher
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        // 执行加密
        byte[] encryptedData = cipher.doFinal(dataBytes);

        return new EncryptionResult(
                encryptedData,
                iv,
                saltBytes,
                EncryptionAlgorithm.AES_CBC,
                dataBytes.length
        );
    }

    /**
     * 使用AES-CBC解密数据
     */
    public String decryptWithAESCBC(EncryptionResult encryptionResult, String password) throws Exception {
        SecretKey key = deriveKey(password, encryptionResult.getSalt());

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(encryptionResult.getIv()));

        byte[] decryptedData = cipher.doFinal(encryptionResult.getEncryptedData());

        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * 生成文件的安全哈希（用于验证文件完整性）
     */
    public String generateFileHash(byte[] data, HashAlgorithm algorithm) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm.getAlgorithmName());
        byte[] hashBytes = digest.digest(data);

        // 转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * 验证文件完整性
     */
    public boolean verifyFileIntegrity(byte[] data, String expectedHash, HashAlgorithm algorithm)
            throws NoSuchAlgorithmException {
        String actualHash = generateFileHash(data, algorithm);
        return actualHash.equals(expectedHash);
    }

    /**
     * 智能加密（压缩后加密）
     */
    public SecureDataResult secureData(String data, String password) throws Exception {
        log.info("开始安全处理数据，数据大小: {} bytes", data.length());

        // 1. 先压缩数据
        DataCompressionUtil compressionUtil = new DataCompressionUtil();
        DataCompressionUtil.CompressionResult compressionResult =
                compressionUtil.smartCompress(data, DataCompressionUtil.CompressionLevel.STANDARD);

        log.debug("数据压缩完成，压缩率: {:.2f}%",
                compressionResult.getCompressionRatio() * 100);

        // 2. 加密压缩后的数据
        String compressedDataStr = new String(compressionResult.getCompressedData(), StandardCharsets.UTF_8);
        EncryptionResult encryptionResult = encryptWithAES(compressedDataStr, password);

        // 3. 生成完整性校验哈希
        String integrityHash = generateFileHash(
                encryptionResult.getEncryptedData(),
                HashAlgorithm.SHA_256
        );

        return new SecureDataResult(
                encryptionResult,
                compressionResult.getAlgorithm(),
                compressionResult.getOriginalSize(),
                compressionResult.getCompressedSize(),
                integrityHash
        );
    }

    /**
     * 解密并解压数据
     */
    public String decryptAndDecompress(SecureDataResult secureDataResult, String password) throws Exception {
        // 1. 验证数据完整性
        String currentHash = generateFileHash(
                secureDataResult.getEncryptionResult().getEncryptedData(),
                HashAlgorithm.SHA_256
        );

        if (!currentHash.equals(secureDataResult.getIntegrityHash())) {
            throw new SecurityException("数据完整性校验失败，数据可能已被篡改");
        }

        // 2. 解密数据
        String decryptedData = decryptWithAES(secureDataResult.getEncryptionResult(), password);

        // 3. 解压数据
        DataCompressionUtil compressionUtil = new DataCompressionUtil();
        byte[] compressedBytes = decryptedData.getBytes(StandardCharsets.UTF_8);
        String decompressedData = compressionUtil.decompress(
                compressedBytes,
                secureDataResult.getCompressionAlgorithm()
        );

        return decompressedData;
    }

    /**
     * 生成随机盐
     */
    private byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    /**
     * 生成随机IV（用于GCM）
     */
    private byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * 生成随机IV（用于CBC）
     */
    private byte[] generateIVForCBC() {
        byte[] iv = new byte[16]; // AES block size
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * 从密码和盐派生密钥
     */
    private SecretKey deriveKey(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(keyDerivationAlgorithm);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterationCount, keyLength);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), AES_ALGORITHM);
    }

    /**
     * 加密结果类
     */
    @Data
    @AllArgsConstructor
    public static class EncryptionResult {
        private byte[] encryptedData;
        private byte[] iv;
        private byte[] salt;
        private EncryptionAlgorithm algorithm;
        private int originalSize;

        public String toBase64String() {
            return Base64.getEncoder().encodeToString(encryptedData);
        }

        public static EncryptionResult fromBase64String(String base64Data, byte[] iv, byte[] salt,
                                                        EncryptionAlgorithm algorithm, int originalSize) {
            byte[] encryptedData = Base64.getDecoder().decode(base64Data);
            return new EncryptionResult(encryptedData, iv, salt, algorithm, originalSize);
        }
    }

    /**
     * 安全数据结果类
     */
    @Data
    @AllArgsConstructor
    public static class SecureDataResult {
        private EncryptionResult encryptionResult;
        private DataCompressionUtil.CompressionAlgorithm compressionAlgorithm;
        private int originalSize;
        private int compressedSize;
        private String integrityHash;

        public double getOverallCompressionRatio() {
            if (originalSize == 0) return 1.0;
            int encryptedSize = encryptionResult.getEncryptedData().length;
            return (double) encryptedSize / originalSize;
        }
    }

    /**
     * 加密算法枚举
     */
    public enum EncryptionAlgorithm {
        AES_GCM("AES/GCM/NoPadding", "AES-GCM加密"),
        AES_CBC("AES/CBC/PKCS5Padding", "AES-CBC加密");

        private final String algorithmName;
        private final String description;

        EncryptionAlgorithm(String algorithmName, String description) {
            this.algorithmName = algorithmName;
            this.description = description;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 哈希算法枚举
     */
    public enum HashAlgorithm {
        MD5("MD5"),
        SHA_1("SHA-1"),
        SHA_256("SHA-256"),
        SHA_512("SHA-512");

        private final String algorithmName;

        HashAlgorithm(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }
    }
}