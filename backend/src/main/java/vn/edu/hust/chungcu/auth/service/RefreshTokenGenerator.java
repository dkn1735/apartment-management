package vn.edu.hust.chungcu.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * Refresh token: 32 byte ngẫu nhiên, base64url không padding (43 ký tự). DB chỉ lưu
 * SHA-256 hex chữ thường của chuỗi đó (spec 0004, mục Token).
 */
@Component
public class RefreshTokenGenerator {

	private static final int SO_BYTE = 32;

	private final SecureRandom random = new SecureRandom();

	public String moi() {
		byte[] bytes = new byte[SO_BYTE];
		this.random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	public static String sha256Hex(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.US_ASCII));
			return HexFormat.of().formatHex(digest);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("JVM không có SHA-256", ex);
		}
	}

}
