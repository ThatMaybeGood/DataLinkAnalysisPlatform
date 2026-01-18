package com.workflow.platform.component.security;

import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 数据加密管理器 - 提供企业级数据加密功能
 */
@Slf4j
@Component
public class DataEncryptionManager {

	private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
	private static final int AES_KEY_SIZE = 256;
	private static final int GCM_TAG_LENGTH = 128;
	private static final int GCM_IV_LENGTH = 12;

	// private static final String RSA_ALGORITHM =
	// "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
	private static final int RSA_KEY_SIZE = 2048;

	// private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
	// private static final int PBKDF2_ITERATIONS = 100000;
	// private static final int PBKDF2_KEY_LENGTH = 256;

	@Value("${workflow.platform.security.encryption.master-key:}")
	private String masterKeyBase64;

	@Value("${workflow.platform.security.encryption.enabled:true}")
	private boolean encryptionEnabled;

	@Value("${workflow.platform.security.encryption.key-rotation-days:90}")
	private int keyRotationDays;

	// 密钥管理
	private SecretKey masterKey;
	private Map<String, KeyPair> keyPairs = new HashMap<>();
	private Map<String, KeyVersion> keyVersions = new HashMap<>();

	// 密钥库
	private KeyStore keyStore;

	@PostConstruct
	public void init() {
		log.info("初始化数据加密管理器");

		try {
			if (encryptionEnabled) {
				initializeMasterKey();
				initializeKeyStore();
				loadExistingKeys();

				log.info("数据加密管理器初始化完成，加密已启用");
			} else {
				log.warn("数据加密未启用，敏感数据将以明文存储");
			}
		} catch (Exception e) {
			log.error("数据加密管理器初始化失败: {}", e.getMessage(), e);
			throw new RuntimeException("加密系统初始化失败", e);
		}
	}

	/**
	 * 加密数据
	 */
	public EncryptedData encrypt(String data, String keyId) {
		if (!encryptionEnabled || data == null) {
			return new EncryptedData(data, false);
		}

		try {
			// 获取加密密钥
			SecretKey secretKey = getEncryptionKey(keyId);

			// 生成随机IV
			byte[] iv = generateIV();

			// 加密数据
			byte[] encrypted = encryptWithAES(data.getBytes(), secretKey, iv);

			// 创建加密数据对象
			EncryptedData encryptedData = new EncryptedData();
			encryptedData.setEncrypted(true);
			encryptedData.setAlgorithm(AES_ALGORITHM);
			encryptedData.setKeyId(keyId);
			encryptedData.setKeyVersion(getKeyVersion(keyId));
			encryptedData.setIv(Base64.getEncoder().encodeToString(iv));
			encryptedData.setData(Base64.getEncoder().encodeToString(encrypted));
			encryptedData.setTimestamp(System.currentTimeMillis());

			// 计算HMAC
			String hmac = calculateHMAC(encryptedData);
			encryptedData.setHmac(hmac);

			return encryptedData;

		} catch (Exception e) {
			log.error("数据加密失败: {}", e.getMessage(), e);
			throw new EncryptionException("数据加密失败", e);
		}
	}

	/**
	 * 解密数据
	 */
	public String decrypt(EncryptedData encryptedData) {
		if (!encryptedData.isEncrypted()) {
			return encryptedData.getData();
		}

		try {
			// 验证HMAC
			verifyHMAC(encryptedData);

			// 获取解密密钥
			String keyId = encryptedData.getKeyId();
			int keyVersion = encryptedData.getKeyVersion();
			SecretKey secretKey = getDecryptionKey(keyId, keyVersion);

			// 解码IV和数据
			byte[] iv = Base64.getDecoder().decode(encryptedData.getIv());
			byte[] encrypted = Base64.getDecoder().decode(encryptedData.getData());

			// 解密数据
			byte[] decrypted = decryptWithAES(encrypted, secretKey, iv);

			return new String(decrypted);

		} catch (Exception e) {
			log.error("数据解密失败: {}", e.getMessage(), e);
			throw new EncryptionException("数据解密失败", e);
		}
	}

	/**
	 * 加密字段（选择性加密）
	 */
	public Map<String, Object> encryptFields(Map<String, Object> data, String[] fieldsToEncrypt, String keyId) {
		if (!encryptionEnabled || fieldsToEncrypt == null || fieldsToEncrypt.length == 0) {
			return data;
		}

		Map<String, Object> result = new HashMap<>(data);

		for (String field : fieldsToEncrypt) {
			if (data.containsKey(field) && data.get(field) != null) {
				String value = data.get(field).toString();
				EncryptedData encrypted = encrypt(value, keyId);
				result.put(field + "_encrypted", encrypted);
				result.remove(field); // 移除明文字段
			}
		}

		return result;
	}

	/**
	 * 解密字段
	 */
	public Map<String, Object> decryptFields(Map<String, Object> data, String[] encryptedFields) {
		if (!encryptionEnabled || encryptedFields == null || encryptedFields.length == 0) {
			return data;
		}

		Map<String, Object> result = new HashMap<>(data);

		for (String field : encryptedFields) {
			String encryptedFieldName = field + "_encrypted";
			if (data.containsKey(encryptedFieldName)) {
				EncryptedData encryptedData = (EncryptedData) data.get(encryptedFieldName);
				String decryptedValue = decrypt(encryptedData);
				result.put(field, decryptedValue);
				result.remove(encryptedFieldName);
			}
		}

		return result;
	}

	/**
	 * 生成数据签名
	 */
	public String signData(String data, String keyId) {
		try {
			KeyPair keyPair = getKeyPair(keyId);
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initSign(keyPair.getPrivate());
			signature.update(data.getBytes());
			byte[] digitalSignature = signature.sign();
			return Base64.getEncoder().encodeToString(digitalSignature);
		} catch (Exception e) {
			log.error("数据签名失败: {}", e.getMessage(), e);
			throw new EncryptionException("数据签名失败", e);
		}
	}

	/**
	 * 验证数据签名
	 */
	public boolean verifySignature(String data, String signatureBase64, String keyId) {
		try {
			KeyPair keyPair = getKeyPair(keyId);
			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initVerify(keyPair.getPublic());
			signature.update(data.getBytes());
			byte[] digitalSignature = Base64.getDecoder().decode(signatureBase64);
			return signature.verify(digitalSignature);
		} catch (Exception e) {
			log.error("签名验证失败: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 生成数字证书
	 */
	public DigitalCertificate generateCertificate(String subject, int validityDays) {
		try {
			// 生成密钥对
			KeyPair keyPair = generateRSAKeyPair();

			// 创建证书信息
			DigitalCertificate certificate = new DigitalCertificate();
			certificate.setSubject(subject);
			certificate.setIssuer("Workflow Platform");
			certificate.setSerialNumber(generateSerialNumber());
			certificate.setValidFrom(System.currentTimeMillis());
			certificate.setValidTo(System.currentTimeMillis() + validityDays * 24L * 60 * 60 * 1000);
			certificate.setPublicKey(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));

			// 生成证书签名
			String certificateData = certificate.toSignableString();
			String signature = signData(certificateData, "ca");
			certificate.setSignature(signature);

			// 存储密钥对
			String keyId = "cert_" + certificate.getSerialNumber();
			keyPairs.put(keyId, keyPair);

			certificate.setKeyId(keyId);

			log.info("生成数字证书: {}", subject);
			return certificate;

		} catch (Exception e) {
			log.error("生成数字证书失败: {}", e.getMessage(), e);
			throw new EncryptionException("生成证书失败", e);
		}
	}

	/**
	 * 密钥轮换
	 */
	public boolean rotateKey(String keyId) {
		try {
			log.info("开始密钥轮换: {}", keyId);

			// 生成新密钥
			SecretKey newKey = generateAESKey();

			// 更新密钥版本
			KeyVersion currentVersion = keyVersions.get(keyId);
			KeyVersion newVersion = new KeyVersion();
			newVersion.setKeyId(keyId);
			newVersion.setVersion(currentVersion != null ? currentVersion.getVersion() + 1 : 1);
			newVersion.setKey(Base64.getEncoder().encodeToString(newKey.getEncoded()));
			newVersion.setCreatedTime(System.currentTimeMillis());
			newVersion.setActive(true);

			// 停用旧密钥
			if (currentVersion != null) {
				currentVersion.setActive(false);
				currentVersion.setDeactivatedTime(System.currentTimeMillis());
			}

			// 保存新密钥
			keyVersions.put(keyId, newVersion);

			// 重新加密使用旧密钥的数据
			reencryptDataWithNewKey(keyId, currentVersion, newVersion);

			log.info("密钥轮换完成: {} -> v{}", keyId, newVersion.getVersion());
			return true;

		} catch (Exception e) {
			log.error("密钥轮换失败: {}，错误: {}", keyId, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 定期密钥轮换任务
	 */
	public void performScheduledKeyRotation() {
		if (!encryptionEnabled) {
			return;
		}

		long currentTime = System.currentTimeMillis();
		long rotationThreshold = keyRotationDays * 24L * 60 * 60 * 1000;

		for (Map.Entry<String, KeyVersion> entry : keyVersions.entrySet()) {
			KeyVersion version = entry.getValue();
			if (version.isActive() &&
					currentTime - version.getCreatedTime() > rotationThreshold) {

				rotateKey(entry.getKey());
			}
		}
	}

	/**
	 * 获取加密统计
	 */
	public EncryptionStats getEncryptionStats() {
		EncryptionStats stats = new EncryptionStats();

		stats.setEncryptionEnabled(encryptionEnabled);
		stats.setTotalKeys(keyVersions.size());
		stats.setActiveKeys((int) keyVersions.values().stream()
				.filter(KeyVersion::isActive)
				.count());

		// 计算加密数据大小统计
		// 在实际应用中，这里应该从数据库统计

		return stats;
	}

	/**
	 * 数据脱敏
	 */
	public String maskSensitiveData(String data, String dataType) {
		if (data == null || data.isEmpty()) {
			return data;
		}

		switch (dataType.toLowerCase()) {
			case "email":
				return maskEmail(data);
			case "phone":
				return maskPhone(data);
			case "idcard":
				return maskIdCard(data);
			case "bankcard":
				return maskBankCard(data);
			case "name":
				return maskName(data);
			default:
				return maskGeneral(data);
		}
	}

	// ========== 私有方法 ==========

	private void initializeMasterKey() throws Exception {
		if (masterKeyBase64 != null && !masterKeyBase64.isEmpty()) {
			// 使用配置的主密钥
			byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
			masterKey = new SecretKeySpec(keyBytes, "AES");
			log.info("使用配置的主密钥");
		} else {
			// 生成新的主密钥
			masterKey = generateAESKey();
			log.warn("生成新的主密钥，请妥善保存: {}",
					Base64.getEncoder().encodeToString(masterKey.getEncoded()));
		}
	}

	private void initializeKeyStore() throws Exception {
		keyStore = KeyStore.getInstance("JCEKS");
		char[] password = "changeit".toCharArray();
		keyStore.load(null, password);
	}

	private void loadExistingKeys() {
		// 从数据库或文件系统加载现有密钥
		// 这里简化为空实现
	}

	private SecretKey generateAESKey() throws NoSuchAlgorithmException {
		KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
		keyGenerator.init(AES_KEY_SIZE);
		return keyGenerator.generateKey();
	}

	private byte[] generateIV() {
		byte[] iv = new byte[GCM_IV_LENGTH];
		new SecureRandom().nextBytes(iv);
		return iv;
	}

	private byte[] encryptWithAES(byte[] data, SecretKey key, byte[] iv) throws Exception {
		Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
		GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
		cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
		return cipher.doFinal(data);
	}

	private byte[] decryptWithAES(byte[] encryptedData, SecretKey key, byte[] iv) throws Exception {
		Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
		GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
		cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
		return cipher.doFinal(encryptedData);
	}

	private SecretKey getEncryptionKey(String keyId) {
		KeyVersion version = keyVersions.get(keyId);
		if (version == null) {
			// 创建新密钥
			version = createNewKeyVersion(keyId);
		}
		return decodeKey(version.getKey());
	}

	private SecretKey getDecryptionKey(String keyId, int keyVersion) {
		// 查找指定版本的密钥
		KeyVersion version = keyVersions.get(keyId + "_v" + keyVersion);
		if (version == null) {
			throw new EncryptionException("密钥版本不存在: " + keyId + " v" + keyVersion);
		}
		return decodeKey(version.getKey());
	}

	private SecretKey decodeKey(String keyBase64) {
		byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
		return new SecretKeySpec(keyBytes, "AES");
	}

	private KeyVersion createNewKeyVersion(String keyId) {
		try {
			SecretKey key = generateAESKey();
			KeyVersion version = new KeyVersion();
			version.setKeyId(keyId);
			version.setVersion(1);
			version.setKey(Base64.getEncoder().encodeToString(key.getEncoded()));
			version.setCreatedTime(System.currentTimeMillis());
			version.setActive(true);

			keyVersions.put(keyId, version);
			return version;
		} catch (Exception e) {
			throw new EncryptionException("创建密钥失败", e);
		}
	}

	private int getKeyVersion(String keyId) {
		KeyVersion version = keyVersions.get(keyId);
		return version != null ? version.getVersion() : 1;
	}

	private String calculateHMAC(EncryptedData data) throws Exception {
		String dataToSign = data.getKeyId() + ":" + data.getKeyVersion() + ":" +
				data.getIv() + ":" + data.getData() + ":" + data.getTimestamp();

		Mac mac = Mac.getInstance("HmacSHA256");
		SecretKeySpec secretKeySpec = new SecretKeySpec(masterKey.getEncoded(), "HmacSHA256");
		mac.init(secretKeySpec);
		byte[] hmacBytes = mac.doFinal(dataToSign.getBytes());
		return Base64.getEncoder().encodeToString(hmacBytes);
	}

	private void verifyHMAC(EncryptedData data) throws Exception {
		String expectedHmac = calculateHMAC(data);
		if (!expectedHmac.equals(data.getHmac())) {
			throw new EncryptionException("HMAC验证失败，数据可能被篡改");
		}
	}

	private KeyPair getKeyPair(String keyId) {
		KeyPair keyPair = keyPairs.get(keyId);
		if (keyPair == null) {
			keyPair = generateRSAKeyPair();
			keyPairs.put(keyId, keyPair);
		}
		return keyPair;
	}

	private KeyPair generateRSAKeyPair() {
		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(RSA_KEY_SIZE);
			return keyPairGenerator.generateKeyPair();
		} catch (Exception e) {
			throw new EncryptionException("生成RSA密钥对失败", e);
		}
	}

	private String generateSerialNumber() {
		return "CERT-" + System.currentTimeMillis() + "-" +
				new SecureRandom().nextInt(10000);
	}

	private void reencryptDataWithNewKey(String keyId, KeyVersion oldVersion, KeyVersion newVersion) {
		// 重新加密使用旧密钥的数据
		// 在实际应用中，这里应该查询数据库并重新加密数据
		log.info("重新加密数据，密钥: {} v{} -> v{}",
				keyId, oldVersion != null ? oldVersion.getVersion() : 0, newVersion.getVersion());
	}

	private String maskEmail(String email) {
		if (email == null || !email.contains("@")) {
			return email;
		}

		String[] parts = email.split("@");
		String username = parts[0];
		String domain = parts[1];

		if (username.length() <= 2) {
			return "***@" + domain;
		}

		String maskedUsername = username.charAt(0) + "***" +
				username.charAt(username.length() - 1);
		return maskedUsername + "@" + domain;
	}

	private String maskPhone(String phone) {
		if (phone == null || phone.length() < 7) {
			return phone;
		}

		String prefix = phone.substring(0, 3);
		String suffix = phone.substring(phone.length() - 4);
		return prefix + "****" + suffix;
	}

	private String maskIdCard(String idCard) {
		if (idCard == null || idCard.length() < 15) {
			return idCard;
		}

		String prefix = idCard.substring(0, 6);
		String suffix = idCard.substring(idCard.length() - 4);
		return prefix + "********" + suffix;
	}

	private String maskBankCard(String bankCard) {
		if (bankCard == null || bankCard.length() < 12) {
			return bankCard;
		}

		String prefix = bankCard.substring(0, 6);
		String suffix = bankCard.substring(bankCard.length() - 4);
		return prefix + "******" + suffix;
	}

	private String maskName(String name) {
		if (name == null || name.length() <= 1) {
			return name;
		}

		if (name.length() == 2) {
			return name.charAt(0) + "*";
		}

		StringBuilder masked = new StringBuilder();
		masked.append(name.charAt(0));
		for (int i = 1; i < name.length() - 1; i++) {
			masked.append("*");
		}
		masked.append(name.charAt(name.length() - 1));
		return masked.toString();
	}

	private String maskGeneral(String data) {
		if (data == null || data.length() <= 4) {
			return "****";
		}

		int visibleLength = Math.max(2, data.length() / 4);
		String visiblePart = data.substring(0, visibleLength);
		return visiblePart + "***";
	}

	// ========== 内部类 ==========

	/**
	 * 加密数据
	 */
	@Data
	public static class EncryptedData {
		private boolean encrypted;
		private String algorithm;
		private String keyId;
		private int keyVersion;
		private String iv;
		private String data;
		private String hmac;
		private long timestamp;

		public EncryptedData() {
		}

		public EncryptedData(String data, boolean encrypted) {
			this.data = data;
			this.encrypted = encrypted;
		}
	}

	/**
	 * 密钥版本
	 */
	@Data
	public static class KeyVersion {
		private String keyId;
		private int version;
		private String key;
		private long createdTime;
		private boolean active;
		private Long deactivatedTime;
		private String rotatedFrom;
	}

	/**
	 * 数字证书
	 */
	@Data
	public static class DigitalCertificate {
		private String serialNumber;
		private String subject;
		private String issuer;
		private long validFrom;
		private long validTo;
		private String publicKey;
		private String signature;
		private String keyId;

		public String toSignableString() {
			return serialNumber + "|" + subject + "|" + issuer + "|" +
					validFrom + "|" + validTo + "|" + publicKey;
		}
	}

	/**
	 * 加密统计
	 */
	@Data
	public static class EncryptionStats {
		private boolean encryptionEnabled;
		private int totalKeys;
		private int activeKeys;
		private long totalEncryptedDataSize;
		private long lastKeyRotationTime;
		private Map<String, Integer> encryptionCountByType;
	}

	/**
	 * 加密异常
	 */
	public static class EncryptionException extends RuntimeException {
		public EncryptionException(String message) {
			super(message);
		}

		public EncryptionException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}