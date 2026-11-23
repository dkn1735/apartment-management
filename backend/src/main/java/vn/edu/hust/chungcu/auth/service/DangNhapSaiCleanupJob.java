package vn.edu.hust.chungcu.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dọn {@code dang_nhap_sai_dinh_danh} cùng lịch với job dọn phiên (spec 0009, AC-12). Lỗi
 * được log ở mức ERROR (chỉ tên lỗi và thông điệp) rồi bỏ qua; lần sau chạy lại.
 */
@Component
public class DangNhapSaiCleanupJob {

	private static final Logger log = LoggerFactory.getLogger(DangNhapSaiCleanupJob.class);

	private final KhoaTamDangNhapService khoaTam;

	public DangNhapSaiCleanupJob(KhoaTamDangNhapService khoaTam) {
		this.khoaTam = khoaTam;
	}

	@Scheduled(cron = "${app.security.session-cleanup-cron}", zone = "${app.timezone}")
	public void chay() {
		try {
			log.info("Dọn định danh đăng nhập sai: daXoa={}", this.khoaTam.donDep());
		}
		catch (RuntimeException ex) {
			log.error("Dọn định danh đăng nhập sai thất bại: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
		}
	}

}
