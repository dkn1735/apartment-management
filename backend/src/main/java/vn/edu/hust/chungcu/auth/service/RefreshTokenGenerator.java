package vn.edu.hust.chungcu.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.common.config.AppProperties;

/**
 * Refresh token: 32 byte, base64url không padding (43 ký tự). DB chỉ lưu SHA-256 hex chữ
 * thường của chuỗi đó (spec 0004, mục Token).
 *
 * <p>
 * Token đầu tiên của một họ phiên là ngẫu nhiên ({@link #moi()}); mỗi token xoay vòng sau
 * đó là {@link #keTiep(String)} của token trước, tính bằng HMAC với một khóa dẫn xuất từ
 * {@code app.security.jwt-secret} (spec 0005, mục Token kế tiếp). Khóa dẫn xuất không bao
 * giờ được log (AC-16).
 */
@Component
public class RefreshTokenGenerator {

	private static final int SO_BYTE = 32;

	private static final String HMAC = "HmacSHA256";

	private static final String NHAN_KHOA_KE_TIEP = "chungcu.refresh-token-ke-tiep.v1";

	private static final Pattern DINH_DANG = Pattern.compile("[A-Za-z0-9_-]{43}");

	private final SecureRandom random = new SecureRandom();

	private final SecretKeySpec khoaKeTiep;

	public RefreshTokenGenerator(AppProperties properties) {
		byte[] goc = properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8);
		byte[] dan = hmac(new SecretKeySpec(goc, HMAC), NHAN_KHOA_KE_TIEP.getBytes(StandardCharsets.US_ASCII));
		this.khoaKeTiep = new SecretKeySpec(dan, HMAC);
	}

	/** Token đầu tiên của một họ phiên (đăng nhập): 32 byte ngẫu nhiên. */
	public String moi() {
		byte[] bytes = new byte[SO_BYTE];
		this.random.nextBytes(bytes);
		return base64Url(bytes);
	}

	/**
	 * Token kế tiếp của {@code token} trong cùng họ phiên: tất định, cùng định dạng với
	 * {@link #moi()}, không đoán được nếu không có khóa dẫn xuất.
	 */
	public String keTiep(String token) {
		return base64Url(hmac(this.khoaKeTiep, token.getBytes(StandardCharsets.US_ASCII)));
	}

	/** Cookie có đúng định dạng refresh token không; kiểm trước mọi truy cập DB. */
	public static boolean dungDinhDang(String token) {
		return token != null && DINH_DANG.matcher(token).matches();
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

	// Mac không an toàn khi dùng chung giữa các luồng, nên mỗi lần gọi tạo một bản mới
	private static byte[] hmac(SecretKeySpec khoa, byte[] duLieu) {
		try {
			Mac mac = Mac.getInstance(HMAC);
			mac.init(khoa);
			return mac.doFinal(duLieu);
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("JVM không có HmacSHA256", ex);
		}
	}

	private static String base64Url(byte[] bytes) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	@Override
	public String toString() {
		return "RefreshTokenGenerator";
	}

}
