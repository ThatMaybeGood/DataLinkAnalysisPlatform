package com.datalink.platform.datasource.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * AesUtil 测试：AES-GCM 往返一致性、密文非明文、错误密钥解密失败。
 */
class AesUtilTest {

    AesUtil util = new AesUtil("0123456789abcdef0123456789abcdef"); // 32 字节

    @Test
    void roundTrip() {
        String c = util.encrypt("p@ssw0rd");
        assertNotEquals("p@ssw0rd", c);
        assertEquals("p@ssw0rd", util.decrypt(c));
    }

    @Test
    void wrongKeyFails() {
        AesUtil other = new AesUtil("fedcba9876543210fedcba9876543210");
        assertThrows(Exception.class, () -> other.decrypt(util.encrypt("x")));
    }
}
