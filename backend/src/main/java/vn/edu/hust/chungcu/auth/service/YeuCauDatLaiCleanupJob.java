package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository;

/**
 * Xóa yêu cầu đặt lại mật khẩu cũ hơn 7 ngày, cùng lịch với job dọn phiên (spec 0010,
 * AC-12). Lỗi được log ở mức ERROR và không ném ra ngoài; lần sau chạy lại bình thường.
 */
@Component
public class YeuCauDatLaiCleanupJob {

	static final Duration GIU_LAI = Duration.ofDays(7);

	private static final Logger log = LoggerFactory.getLogger(YeuCauDatLaiCleanupJob.class);

	private final QuenMatKhauRepository repository;

	private final Clock clock;

	public YeuCauDatLaiCleanupJob(QuenMatKhauRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Scheduled(cron = "${app.security.session-cleanup-cron}", zone = "${app.timezone}")
	public void chay() {
		try {
			int daXoa = this.repository.xoaTruoc(Instant.now(this.clock).minus(GIU_LAI));
			log.info("Dọn yêu cầu đặt lại mật khẩu: daXoa={}", daXoa);
		}
		catch (RuntimeException ex) {
			log.error("Dọn yêu cầu đặt lại mật khẩu thất bại: {}", ex.getClass().getName());
		}
	}

}
