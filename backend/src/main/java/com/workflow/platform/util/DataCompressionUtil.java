package com.workflow.platform.util;

/**
 * @author Mine
 * @version 1.0
 * 描述:
 * @date 2026/1/10 23:48
 */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

/**
 * 数据压缩工具类
 */
@Component
@Slf4j
public class DataCompressionUtil {

    /**
     * 使用GZIP压缩字符串
     */
    public byte[] compressWithGzip(String data) throws IOException {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOS = new GZIPOutputStream(bos)) {
            gzipOS.write(data.getBytes(StandardCharsets.UTF_8));
        }

        byte[] compressed = bos.toByteArray();
        log.debug("GZIP压缩完成，原始大小: {}，压缩后大小: {}",
                data.length(), compressed.length);
        return compressed;
    }

    /**
     * 使用GZIP解压字节数组
     */
    public String decompressWithGzip(byte[] compressedData) throws IOException {
        if (compressedData == null || compressedData.length == 0) {
            return "";
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(compressedData);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try (GZIPInputStream gzipIS = new GZIPInputStream(bis)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIS.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
        }

        return bos.toString(StandardCharsets.UTF_8.name());
    }

    /**
     * 使用Deflater压缩字符串
     */
    public byte[] compressWithDeflater(String data) throws IOException {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }

        byte[] input = data.getBytes(StandardCharsets.UTF_8);
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(input);
        deflater.finish();

        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            bos.write(buffer, 0, count);
        }
        deflater.end();

        byte[] compressed = bos.toByteArray();
        log.debug("Deflater压缩完成，原始大小: {}，压缩后大小: {}",
                input.length, compressed.length);
        return compressed;
    }

    /**
     * 使用Inflater解压字节数组
     */
    public String decompressWithInflater(byte[] compressedData) throws IOException {
        if (compressedData == null || compressedData.length == 0) {
            return "";
        }

        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(compressedData.length);
        byte[] buffer = new byte[1024];

        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                bos.write(buffer, 0, count);
            }
        } catch (DataFormatException e) {
            throw new IOException("数据解压失败", e);
        } finally {
            inflater.end();
        }

        return bos.toString(StandardCharsets.UTF_8.name());
    }

    /**
     * 智能压缩（根据数据大小选择算法）
     */
    public CompressionResult smartCompress(String data, CompressionLevel level) {
        try {
            if (data.length() < 1024) {
                // 小数据不压缩
                log.debug("数据小于1KB，不进行压缩");
                return new CompressionResult(
                        data.getBytes(StandardCharsets.UTF_8),
                        CompressionAlgorithm.NONE,
                        data.length(),
                        data.length()
                );
            }

            byte[] compressed;
            CompressionAlgorithm algorithm;

            switch (level) {
                case HIGH:
                    compressed = compressWithDeflater(data);
                    algorithm = CompressionAlgorithm.DEFLATER;
                    break;
                case STANDARD:
                default:
                    compressed = compressWithGzip(data);
                    algorithm = CompressionAlgorithm.GZIP;
                    break;
            }

            return new CompressionResult(
                    compressed,
                    algorithm,
                    data.length(),
                    compressed.length
            );
        } catch (IOException e) {
            log.error("数据压缩失败", e);
            // 压缩失败时返回原始数据
            return new CompressionResult(
                    data.getBytes(StandardCharsets.UTF_8),
                    CompressionAlgorithm.NONE,
                    data.length(),
                    data.length()
            );
        }
    }

    /**
     * 解压数据
     */
    public String decompress(byte[] compressedData, CompressionAlgorithm algorithm) throws IOException {
        if (algorithm == null) {
            // 尝试自动检测
            algorithm = detectCompressionAlgorithm(compressedData);
        }

        switch (algorithm) {
            case GZIP:
                return decompressWithGzip(compressedData);
            case DEFLATER:
                return decompressWithInflater(compressedData);
            case NONE:
            default:
                return new String(compressedData, StandardCharsets.UTF_8);
        }
    }

    /**
     * 检测压缩算法
     */
    public CompressionAlgorithm detectCompressionAlgorithm(byte[] data) {
        if (data == null || data.length < 2) {
            return CompressionAlgorithm.NONE;
        }

        // 检查GZIP魔数
        if (data[0] == (byte) 0x1f && data[1] == (byte) 0x8b) {
            return CompressionAlgorithm.GZIP;
        }

        // 检查Deflater/Zlib
        if (data.length >= 2 && (data[0] & 0x0F) == 0x08) {
            // 检查CMF字节（压缩方法和压缩信息）
            int cmf = data[0] & 0xFF;
            if ((cmf & 0x0F) == 8) { // CM=8 表示deflate
                return CompressionAlgorithm.DEFLATER;
            }
        }

        return CompressionAlgorithm.NONE;
    }

    /**
     * 压缩结果类
     */
    @Data
    @AllArgsConstructor
    public static class CompressionResult {
        private byte[] compressedData;
        private CompressionAlgorithm algorithm;
        private int originalSize;
        private int compressedSize;

        public double getCompressionRatio() {
            if (originalSize == 0) return 1.0;
            return (double) compressedSize / originalSize;
        }

        public boolean isCompressed() {
            return algorithm != CompressionAlgorithm.NONE;
        }
    }

    /**
     * 压缩算法枚举
     */
    public enum CompressionAlgorithm {
        NONE("none", "不压缩"),
        GZIP("gzip", "GZIP压缩"),
        DEFLATER("deflater", "Deflater压缩");

        private final String code;
        private final String description;

        CompressionAlgorithm(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 压缩级别枚举
     */
    public enum CompressionLevel {
        STANDARD("standard", "标准压缩"),
        HIGH("high", "高压缩率"),
        FAST("fast", "快速压缩");

        private final String code;
        private final String description;

        CompressionLevel(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}