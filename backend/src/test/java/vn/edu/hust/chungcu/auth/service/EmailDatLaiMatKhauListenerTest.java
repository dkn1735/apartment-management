package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import vn.edu.hust.chungcu.common.config.AppProperties;
import vn.edu.hust.chungcu.common.email.EmailGui;

/**
 * Listener gửi email đặt lại mật khẩu (spec 0010, AC-1, AC-3), với bản giả trong bộ nhớ.
 */
@ExtendWith(OutputCaptureExtension.class)
class EmailDatLaiMatKhauListenerTest {

	private static final String TOKEN = "T".repeat(43);

	private final List<EmailGui> daGui = new ArrayList<>();

	private final EmailDatLaiMatKhau mau = new EmailDatLaiMatKhau(new AppProperties(ZoneId.of("Asia/Ho_Chi_Minh"),
			new AppProperties.Security("k".repeat(32), Duration.ofMinutes(15), Duration.ofDays(7), 4, true,
					Duration.ofDays(30), Duration.ofSeconds(30), Duration.ofDays(30), "0 0 3 * * *",
					Duration.ofMinutes(30)),
			"https://chungcu.vn", new AppProperties.Mail("x@chungcu.vn")));

	private final YeuCauDatLaiDaTao suKien = new YeuCauDatLaiDaTao(42, "ND00000001", "an@chungcu.test", "An <b>&</b>",
			TOKEN, Instant.parse("2026-11-16T01:30:00Z"));

	@Test
	void dungEmailCoLinkOCaHaiPhanVaEscapeHoTen() {
		new EmailDatLaiMatKhauListener(this.daGui::add, this.mau, Runnable::run).khiTao(this.suKien);
		assertThat(this.daGui).singleElement().satisfies(e -> {
			assertThat(e.nguoiNhan()).isEqualTo("an@chungcu.test");
			assertThat(e.chuThuong()).contains("https://chungcu.vn/dat-lai-mat-khau#token=" + TOKEN)
				.contains("Xin chào An <b>&</b>,")
				.contains("08:30 ngày 16/11/2026");
			assertThat(e.html()).contains("https://chungcu.vn/dat-lai-mat-khau#token=" + TOKEN)
				.contains("An &lt;b&gt;&amp;&lt;/b&gt;")
				.doesNotContain("<b>&</b>");
		});
		assertThat(this.suKien.toString()).doesNotContain(TOKEN).doesNotContain("an@chungcu.test");
	}

	@Test
	void hangDoiDayThiBoEmailVaLogMaYeuCau(CapturedOutput output) {
		TaskExecutor tuChoi = task -> {
			throw new TaskRejectedException("đầy");
		};
		new EmailDatLaiMatKhauListener(this.daGui::add, this.mau, tuChoi).khiTao(this.suKien);
		assertThat(this.daGui).isEmpty();
		assertThat(output.getAll()).contains("ERROR")
			.contains("hàng đợi gửi đã đầy: maYeuCau=42")
			.doesNotContain(TOKEN)
			.doesNotContain("an@chungcu.test");
	}

}
