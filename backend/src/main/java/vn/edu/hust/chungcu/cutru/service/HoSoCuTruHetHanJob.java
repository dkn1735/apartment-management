package vn.edu.hust.chungcu.cutru.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Chạy {@link HetHanHoSoCuTruService} đầu mỗi ngày theo {@code app.timezone}, và chạy bù
 * một lần khi ứng dụng sẵn sàng để không lệch trạng thái căn nếu server tắt qua mốc giờ
 * chạy (spec 0014, AC-11).
 */
@Component
public class HoSoCuTruHetHanJob {

	private static final Logger log = LoggerFactory.getLogger(HoSoCuTruHetHanJob.class);

	private final HetHanHoSoCuTruService hetHan;

	public HoSoCuTruHetHanJob(HetHanHoSoCuTruService hetHan) {
		this.hetHan = hetHan;
	}

	@Scheduled(cron = "${app.cutru.residency-expiry-cron}", zone = "${app.timezone}")
	public void chay() {
		try {
			log.info("Hết hạn hồ sơ cư trú: soHoSo={}", this.hetHan.chay());
		}
		catch (RuntimeException ex) {
			log.error("Hết hạn hồ sơ cư trú thất bại: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
		}
	}

	@EventListener(ApplicationReadyEvent.class)
	public void chayBu() {
		chay();
	}

}
