package vn.edu.hust.chungcu.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.common.error.ApiException;

/**
 * Ngưỡng và thời gian của khóa tạm đăng nhập (spec 0009). Frontend giữ cùng giá trị ở
 * {@code src/lib/khoa-tam.ts} cho câu gợi ý.
 */
public final class KhoaTamDangNhap {

	/** Lượt sai thứ {@code N} liên tiếp thì khóa (N). */
	public static final int SO_LAN_SAI_TOI_DA = 5;

	/** Thời gian khóa, cũng là cửa sổ đếm (T). */
	public static final Duration THOI_GIAN_KHOA = Duration.ofMinutes(15);

	private KhoaTamDangNhap() {
	}

	/** Số giây còn lại tới {@code khoaDen}, làm tròn lên, nhỏ nhất là 1 (AC-7). */
	public static long retryAfter(Instant now, Instant khoaDen) {
		long ms = Duration.between(now, khoaDen).toMillis();
		return Math.max(1, Math.ceilDiv(ms, 1000L));
	}

	/**
	 * Lỗi 429 kèm {@code Retry-After}; tài khoản thật và định danh ảo cho ra cùng
	 * response (AC-7).
	 */
	public static ApiException quaNhieuLan(Instant now, Instant khoaDen) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.RETRY_AFTER, Long.toString(retryAfter(now, khoaDen)));
		return new ApiException(HttpStatus.TOO_MANY_REQUESTS, AuthErrorCodes.AUTH_TOO_MANY_ATTEMPTS,
				AuthErrorCodes.AUTH_TOO_MANY_ATTEMPTS_MESSAGE, List.of(), headers);
	}

}
