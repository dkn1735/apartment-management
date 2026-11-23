package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/** {@code Retry-After} và job dọn (spec 0009, AC-7, AC-12). */
@ExtendWith(OutputCaptureExtension.class)
class KhoaTamDangNhapTest {

	private static final Instant NOW = Instant.parse("2026-11-16T01:00:00Z");

	@Test
	void retryAfterLamTronLenNhoNhatLaMot() {
		assertThat(KhoaTamDangNhap.retryAfter(NOW, NOW.plus(Duration.ofMinutes(15)))).isEqualTo(900);
		assertThat(KhoaTamDangNhap.retryAfter(NOW, NOW.plusMillis(1001))).isEqualTo(2);
		assertThat(KhoaTamDangNhap.retryAfter(NOW, NOW.plusNanos(1000))).isEqualTo(1);
		assertThat(KhoaTamDangNhap.retryAfter(NOW, NOW)).isEqualTo(1);
	}

	@Test
	void quaNhieuLanCoRetryAfter() {
		var loi = KhoaTamDangNhap.quaNhieuLan(NOW, NOW.plusSeconds(300));
		assertThat(loi.getStatus().value()).isEqualTo(429);
		assertThat(loi.getErrorCode()).isEqualTo("AUTH_TOO_MANY_ATTEMPTS");
		assertThat(loi.getHeaders().getFirst("Retry-After")).isEqualTo("300");
	}

	@Test
	void jobDonLoiThiLogErrorVaKhongNem(CapturedOutput output) {
		KhoaTamDangNhapService service = mock(KhoaTamDangNhapService.class);
		when(service.donDep()).thenThrow(new IllegalStateException("DB tạm thời không kết nối được"));
		assertThatCode(new DangNhapSaiCleanupJob(service)::chay).doesNotThrowAnyException();
		assertThat(output.getAll()).contains("ERROR")
			.contains("Dọn định danh đăng nhập sai thất bại: IllegalStateException: DB tạm thời không kết nối được");
	}

}
