package vn.edu.hust.chungcu.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dọn {@code phien_dang_nhap} mỗi ngày (spec 0005, AC-15). Lỗi được log ở mức ERROR (chỉ
 * tên lỗi và thông điệp) để không bị nuốt im lặng; lần sau chạy lại bình thường.
 */
@Component
public class PhienDangNhapCleanupJob {

	private static final Logger log = LoggerFactory.getLogger(PhienDangNhapCleanupJob.class);

	private final PhienDangNhapService phienService;

	public PhienDangNhapCleanupJob(PhienDangNhapService phienService) {
		this.phienService = phienService;
	}

	@Scheduled(cron = "${app.security.session-cleanup-cron}", zone = "${app.timezone}")
	public void chay() {
		try {
			log.info("Dọn phiên: daXoa={}", this.phienService.donDep());
		}
		catch (RuntimeException ex) {
			log.error("Dọn phiên thất bại: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
		}
	}

}
