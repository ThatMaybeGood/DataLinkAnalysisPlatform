package com.datalink.platform.datasource.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 连接器密码 AES-GCM 加解密工具。
 * 密文格式：12 字节随机 IV 前置 + GCM 密文（含认证标签），整体 Base64 输出。
 */
@Component
public class AesUtil {

    private static final String TRANSFORM = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;

    public AesUtil(@Value("${datalink.crypto.key}") String keyRaw) {
        byte[] bytes = keyRaw.getBytes(StandardCharsets.UTF_8);
        if (bytes.length != 16 && bytes.length != 24 && bytes.length != 32) {
            throw new IllegalStateException("datalink.crypto.key 需 16/24/32 字节");
        }
        this.key = new SecretKeySpec(bytes, "AES");
    }

    public String encrypt(String plain) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_LENGTH + enc.length];
            System.arraycopy(iv, 0, out, 0, IV_LENGTH);
            System.arraycopy(enc, 0, out, IV_LENGTH, enc.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("AES 加密失败", e);
        }
    }

    public String decrypt(String cipher) {
        try {
            byte[] all = Base64.getDecoder().decode(cipher);
            byte[] iv = new byte[IV_LENGTH];
            byte[] enc = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            System.arraycopy(all, IV_LENGTH, enc, 0, enc.length);
            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(c.doFinal(enc), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES 解密失败", e);
        }
    }
}
