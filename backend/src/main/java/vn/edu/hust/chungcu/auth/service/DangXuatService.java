package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.repository.PhienDangNhapRepository;

/**
 * Đăng xuất (spec 0005, AC-8): thu hồi mọi dòng còn hiệu lực của họ phiên mà cookie trỏ
 * tới, dưới khóa theo họ. Không ném lỗi nghiệp vụ nào; các họ phiên khác của cùng người
 * dùng không bị đụng.
 */
@Service
public class DangXuatService {

	private static final Logger log = LoggerFactory.getLogger(DangXuatService.class);

	private final PhienDangNhapRepository phien;

	private final Clock clock;

	public DangXuatService(PhienDangNhapRepository phien, Clock clock) {
		this.phien = phien;
		this.clock = clock;
	}

	@Transactional
	public void dangXuat(String refreshToken) {
		if (refreshToken == null || refreshToken.isBlank()) {
			return;
		}
		Optional<UUID> maHoPhien = this.phien.timHoPhien(RefreshTokenGenerator.sha256Hex(refreshToken));
		if (maHoPhien.isEmpty()) {
			return;
		}
		this.phien.khoaHo(maHoPhien.get());
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		int soDong = this.phien.thuHoiHo(maHoPhien.get(), now, LyDoThuHoi.DANG_XUAT);
		if (soDong > 0) {
			log.info("Đăng xuất: maHoPhien={}", maHoPhien.get());
		}
	}

}
