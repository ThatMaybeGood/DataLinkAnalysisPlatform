package com.workflow.platform.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.zip.*;

/**
 * 增强的加密和压缩工具
 */
@Slf4j
@Component
public class EnhancedEncryptionUtil {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private static final String RSA_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    private static final int RSA_KEY_SIZE = 2048;

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int PBKDF2_SALT_LENGTH = 16;

    @Value("${workflow.platform.encryption.enabled:true}")
    private boolean encryptionEnabled;

    @Value("${workflow.platform.encryption.aes-key:}")
    private String aesKeyBase64;

    @Value("${workflow.platform.encryption.rsa-public-key:}")
    private String rsaPublicKeyBase64;

    @Value("${workflow.platform.encryption.rsa-private-key:}")
    private String rsaPrivateKeyBase64;

    @Value("${workflow.platform.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${workflow.platform.compression.algorithm:gzip}")
    private String compressionAlgorithm;

    @Value("${workflow.platform.compression.level:6}")
    private int compressionLevel;

    /**
     * AES-GCM 加密
     */
    public byte[] encryptAES(byte[] data, String password) throws Exception {
        if (!encryptionEnabled) {
            return data;
        }

        try {
            // 生成随机盐和IV
            byte[] salt = generateRandomBytes(PBKDF2_SALT_LENGTH);
            byte[] iv = generateRandomBytes(GCM_IV_LENGTH);

            // 从密码派生密钥
            SecretKey secretKey = deriveKeyFromPassword(password, salt);

            // 配置GCM参数
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

            // 加密数据
            byte[] cipherText = cipher.doFinal(data);

            // 组合结果: 盐 + IV + 密文
            ByteBuffer byteBuffer = ByteBuffer.allocate(
                    salt.length + iv.length + cipherText.length
            );
            byteBuffer.put(salt);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return byteBuffer.array();

        } catch (Exception e) {
            log.error("AES加密失败", e);
            throw new SecurityException("加密失败", e);
        }
    }

    /**
     * AES-GCM 解密
     */
    public byte[] decryptAES(byte[] encryptedData, String password) throws Exception {
        if (!encryptionEnabled) {
            return encryptedData;
        }

        try {
            ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);

            // 提取盐、IV和密文
            byte[] salt = new byte[PBKDF2_SALT_LENGTH];
            byteBuffer.get(salt);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            // 从密码派生密钥
            SecretKey secretKey = deriveKeyFromPassword(password, salt);

            // 配置GCM参数
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

            // 解密数据
            return cipher.doFinal(cipherText);

        } catch (Exception e) {
            log.error("AES解密失败", e);
            throw new SecurityException("解密失败", e);
        }
    }

    /**
     * RSA 加密
     */
    public byte[] encryptRSA(byte[] data) throws Exception {
        if (!encryptionEnabled || rsaPublicKeyBase64.isEmpty()) {
            return data;
        }

        try {
            // 解码公钥
            byte[] keyBytes = Base64.getDecoder().decode(rsaPublicKeyBase64);
            PublicKey publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new java.security.spec.X509EncodedKeySpec(keyBytes));

            // 加密数据
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            // RSA有长度限制，需要分块加密
            int blockSize = RSA_KEY_SIZE / 8 - 66; // OAEP填充占用空间
            int offset = 0;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while (offset < data.length) {
                int length = Math.min(blockSize, data.length - offset);
                byte[] block = cipher.doFinal(data, offset, length);
                outputStream.write(block);
                offset += length;
            }

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("RSA加密失败", e);
            throw new SecurityException("RSA加密失败", e);
        }
    }

    /**
     * RSA 解密
     */
    public byte[] decryptRSA(byte[] encryptedData) throws Exception {
        if (!encryptionEnabled || rsaPrivateKeyBase64.isEmpty()) {
            return encryptedData;
        }

        try {
            // 解码私钥
            byte[] keyBytes = Base64.getDecoder().decode(rsaPrivateKeyBase64);
            PrivateKey privateKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(keyBytes));

            // 解密数据
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);

            // 分块解密
            int blockSize = RSA_KEY_SIZE / 8;
            int offset = 0;
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            while (offset < encryptedData.length) {
                int length = Math.min(blockSize, encryptedData.length - offset);
                byte[] block = cipher.doFinal(encryptedData, offset, length);
                outputStream.write(block);
                offset += length;
            }

            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("RSA解密失败", e);
            throw new SecurityException("RSA解密失败", e);
        }
    }

    /**
     * 混合加密：使用RSA加密AES密钥，AES加密数据
     */
    public EncryptedData hybridEncrypt(byte[] data, String aesPassword) throws Exception {
        if (!encryptionEnabled) {
            return new EncryptedData(data, null, "none");
        }

        try {
            // 生成随机的AES密钥
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE);
            SecretKey aesKey = keyGen.generateKey();
            byte[] aesKeyBytes = aesKey.getEncoded();

            // 使用RSA加密AES密钥
            byte[] encryptedAesKey = encryptRSA(aesKeyBytes);

            // 使用AES密钥加密数据
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            byte[] iv = generateRandomBytes(GCM_IV_LENGTH);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmParameterSpec);
            byte[] encryptedData = cipher.doFinal(data);

            // 创建加密数据对象
            EncryptedData result = new EncryptedData();
            result.setData(encryptedData);
            result.setEncryptedKey(encryptedAesKey);
            result.setIv(iv);
            result.setAlgorithm("RSA-AES-GCM");
            result.setTimestamp(System.currentTimeMillis());

            return result;

        } catch (Exception e) {
            log.error("混合加密失败", e);
            throw new SecurityException("混合加密失败", e);
        }
    }

    /**
     * 混合解密
     */
    public byte[] hybridDecrypt(EncryptedData encryptedData) throws Exception {
        if (!encryptionEnabled) {
            return encryptedData.getData();
        }

        try {
            // 使用RSA解密AES密钥
            byte[] aesKeyBytes = decryptRSA(encryptedData.getEncryptedKey());
            SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

            // 使用AES密钥解密数据
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(
                    GCM_TAG_LENGTH, encryptedData.getIv()
            );
            cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmParameterSpec);

            return cipher.doFinal(encryptedData.getData());

        } catch (Exception e) {
            log.error("混合解密失败", e);
            throw new SecurityException("混合解密失败", e);
        }
    }

    /**
     * GZIP 压缩
     */
    public byte[] compressGzip(byte[] data) throws Exception {
        if (!compressionEnabled) {
            return data;
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {

            gzip.write(data);
            gzip.finish();
            return bos.toByteArray();

        } catch (Exception e) {
            log.error("GZIP压缩失败", e);
            throw new RuntimeException("压缩失败", e);
        }
    }

    /**
     * GZIP 解压
     */
    public byte[] decompressGzip(byte[] compressedData) throws Exception {
        if (!compressionEnabled) {
            return compressedData;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
             GZIPInputStream gzip = new GZIPInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzip.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();

        } catch (Exception e) {
            log.error("GZIP解压失败", e);
            throw new RuntimeException("解压失败", e);
        }
    }

    /**
     * DEFLATE 压缩
     */
    public byte[] compressDeflate(byte[] data, int level) throws Exception {
        if (!compressionEnabled) {
            return data;
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DeflaterOutputStream deflater = new DeflaterOutputStream(bos,
                     new Deflater(level))) {

            deflater.write(data);
            deflater.finish();
            return bos.toByteArray();

        } catch (Exception e) {
            log.error("DEFLATE压缩失败", e);
            throw new RuntimeException("压缩失败", e);
        }
    }

    /**
     * DEFLATE 解压
     */
    public byte[] decompressDeflate(byte[] compressedData) throws Exception {
        if (!compressionEnabled) {
            return compressedData;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
             InflaterInputStream inflater = new InflaterInputStream(bis);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inflater.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();

        } catch (Exception e) {
            log.error("DEFLATE解压失败", e);
            throw new RuntimeException("解压失败", e);
        }
    }

    /**
     * LZ4 压缩（快速压缩算法）
     */
    public byte[] compressLZ4(byte[] data) throws Exception {
        if (!compressionEnabled) {
            return data;
        }

        try {
            // 这里使用简化的LZ4实现
            // 实际项目中应该使用 org.lz4:lz4-java 库
            return compressDeflate(data, Deflater.BEST_SPEED);
        } catch (Exception e) {
            log.error("LZ4压缩失败", e);
            throw new RuntimeException("压缩失败", e);
        }
    }

    /**
     * LZ4 解压
     */
    public byte[] decompressLZ4(byte[] compressedData) throws Exception {
        if (!compressionEnabled) {
            return compressedData;
        }

        try {
            return decompressDeflate(compressedData);
        } catch (Exception e) {
            log.error("LZ4解压失败", e);
            throw new RuntimeException("解压失败", e);
        }
    }

    /**
     * 智能压缩：根据数据特性选择最佳算法
     */
    public CompressedData smartCompress(byte[] data) throws Exception {
        if (!compressionEnabled) {
            return new CompressedData(data, "none", data.length, data.length);
        }

        try {
            // 分析数据特性
            double entropy = calculateEntropy(data);
            int originalSize = data.length;

            CompressedData bestResult = null;
            double bestRatio = 0;

            // 尝试不同的压缩算法
            String[] algorithms = {"gzip", "deflate", "lz4"};

            for (String algorithm : algorithms) {
                byte[] compressed;
                switch (algorithm) {
                    case "gzip":
                        compressed = compressGzip(data);
                        break;
                    case "deflate":
                        compressed = compressDeflate(data, compressionLevel);
                        break;
                    case "lz4":
                        compressed = compressLZ4(data);
                        break;
                    default:
                        continue;
                }

                double ratio = (double) compressed.length / originalSize;

                // 选择压缩率最好的算法
                if (bestResult == null || ratio < bestRatio) {
                    bestRatio = ratio;
                    bestResult = new CompressedData(
                            compressed, algorithm, originalSize, compressed.length
                    );
                }
            }

            // 如果压缩效果不好（压缩率>90%），则不压缩
            if (bestRatio > 0.9) {
                return new CompressedData(data, "none", originalSize, originalSize);
            }

            return bestResult;

        } catch (Exception e) {
            log.error("智能压缩失败", e);
            throw new RuntimeException("压缩失败", e);
        }
    }

    /**
     * 智能解压
     */
    public byte[] smartDecompress(CompressedData compressedData) throws Exception {
        if (!compressionEnabled || "none".equals(compressedData.getAlgorithm())) {
            return compressedData.getData();
        }

        try {
            switch (compressedData.getAlgorithm()) {
                case "gzip":
                    return decompressGzip(compressedData.getData());
                case "deflate":
                    return decompressDeflate(compressedData.getData());
                case "lz4":
                    return decompressLZ4(compressedData.getData());
                default:
                    throw new IllegalArgumentException("不支持的压缩算法: " +
                            compressedData.getAlgorithm());
            }
        } catch (Exception e) {
            log.error("智能解压失败", e);
            throw new RuntimeException("解压失败", e);
        }
    }

    /**
     * 加密并压缩数据
     */
    public SecureData encryptAndCompress(byte[] data, String password) throws Exception {
        try {
            // 先压缩
            CompressedData compressed = smartCompress(data);

            // 再加密
            EncryptedData encrypted = hybridEncrypt(compressed.getData(), password);

            SecureData secureData = new SecureData();
            secureData.setEncryptedData(encrypted);
            secureData.setCompressionInfo(compressed);
            secureData.setOriginalSize(data.length);
            secureData.setTimestamp(System.currentTimeMillis());

            return secureData;

        } catch (Exception e) {
            log.error("加密压缩失败", e);
            throw new SecurityException("加密压缩失败", e);
        }
    }

    /**
     * 解密并解压数据
     */
    public byte[] decryptAndDecompress(SecureData secureData, String password) throws Exception {
        try {
            // 先解密
            byte[] decrypted = hybridDecrypt(secureData.getEncryptedData());

            // 再解压
            CompressedData compressed = secureData.getCompressionInfo();
            compressed.setData(decrypted);
            return smartDecompress(compressed);

        } catch (Exception e) {
            log.error("解密解压失败", e);
            throw new SecurityException("解密解压失败", e);
        }
    }

    /**
     * 生成密钥对
     */
    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(RSA_KEY_SIZE);
        return keyGen.generateKeyPair();
    }

    /**
     * 生成随机字节
     */
    public byte[] generateRandomBytes(int length) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }

    /**
     * 从密码派生密钥
     */
    private SecretKey deriveKeyFromPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt,
                PBKDF2_ITERATIONS, AES_KEY_SIZE);
        SecretKey secretKey = factory.generateSecret(spec);
        return new SecretKeySpec(secretKey.getEncoded(), "AES");
    }

    /**
     * 计算数据熵（用于判断压缩效果）
     */
    private double calculateEntropy(byte[] data) {
        if (data.length == 0) return 0;

        int[] frequency = new int[256];
        for (byte b : data) {
            frequency[b & 0xFF]++;
        }

        double entropy = 0;
        for (int count : frequency) {
            if (count > 0) {
                double probability = (double) count / data.length;
                entropy -= probability * (Math.log(probability) / Math.log(2));
            }
        }

        return entropy;
    }

    // ========== 内部类 ==========

    /**
     * 加密数据
     */
    @Data
    public static class EncryptedData {
        private byte[] data;
        private byte[] encryptedKey;
        private byte[] iv;
        private String algorithm;
        private long timestamp;
        private Map<String, Object> metadata;

        public EncryptedData(byte[] data, Object o, String none) {

        }

        public EncryptedData() {

        }
    }

    /**
     * 压缩数据
     */
    @Data
    public static class CompressedData {
        private byte[] data;
        private String algorithm;
        private int originalSize;
        private int compressedSize;

        public CompressedData() {}

        public CompressedData(byte[] data, String algorithm,
                              int originalSize, int compressedSize) {
            this.data = data;
            this.algorithm = algorithm;
            this.originalSize = originalSize;
            this.compressedSize = compressedSize;
        }

        public double getCompressionRatio() {
            return (double) compressedSize / originalSize;
        }

        public int getSpaceSaved() {
            return originalSize - compressedSize;
        }
    }

    /**
     * 安全数据（加密+压缩）
     */
    @Data
    public static class SecureData {
        private EncryptedData encryptedData;
        private CompressedData compressionInfo;
        private int originalSize;
        private long timestamp;
        private String integrityHash;

        public boolean verifyIntegrity() {
            // 验证数据完整性
            try {
                String currentHash = calculateHash();
                return currentHash.equals(integrityHash);
            } catch (Exception e) {
                return false;
            }
        }

        private String calculateHash() throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] combined = new byte[encryptedData.getData().length +
                    encryptedData.getEncryptedKey().length];

            System.arraycopy(encryptedData.getData(), 0, combined, 0,
                    encryptedData.getData().length);
            System.arraycopy(encryptedData.getEncryptedKey(), 0, combined,
                    encryptedData.getData().length, encryptedData.getEncryptedKey().length);

            byte[] hash = digest.digest(combined);
            return Base64.getEncoder().encodeToString(hash);
        }
    }

    /**
     * 性能统计
     */
    @Data
    public static class PerformanceStats {
        private long encryptionTime;
        private long decryptionTime;
        private long compressionTime;
        private long decompressionTime;
        private double encryptionThroughput; // MB/s
        private double decryptionThroughput; // MB/s
        private double compressionRatio;
        private int processedSize;

        public void calculateThroughput(int dataSize) {
            if (encryptionTime > 0) {
                encryptionThroughput = (dataSize / 1024.0 / 1024.0) /
                        (encryptionTime / 1000.0);
            }
            if (decryptionTime > 0) {
                decryptionThroughput = (dataSize / 1024.0 / 1024.0) /
                        (decryptionTime / 1000.0);
            }
        }
    }
}