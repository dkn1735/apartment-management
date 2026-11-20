package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.repository.HoPhienCuaToken;
import vn.edu.hust.chungcu.auth.repository.PhienDangNhapRepository;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

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

	private final NhatKyService nhatKy;

	public DangXuatService(PhienDangNhapRepository phien, Clock clock, NhatKyService nhatKy) {
		this.phien = phien;
		this.clock = clock;
		this.nhatKy = nhatKy;
	}

	@Transactional
	public void dangXuat(String refreshToken) {
		// Sai định dạng coi như không có cookie, không chạm DB
		if (!RefreshTokenGenerator.dungDinhDang(refreshToken)) {
			return;
		}
		Optional<HoPhienCuaToken> timThay = this.phien
			.timHoPhienVaNguoiDung(RefreshTokenGenerator.sha256Hex(refreshToken));
		if (timThay.isEmpty()) {
			return;
		}
		HoPhienCuaToken ho = timThay.get();
		this.phien.khoaHo(ho.maHoPhien());
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		int soDong = this.phien.thuHoiHo(ho.maHoPhien(), now, LyDoThuHoi.DANG_XUAT);
		if (soDong > 0) {
			log.info("Đăng xuất: maHoPhien={}", ho.maHoPhien());
			// Đang trong transaction: dòng nhật ký chờ tới sau commit (spec 0007, AC-4)
			this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_DANG_XUAT, ho.maNguoiDung(),
					DoiTuongTacDong.nguoiDung(ho.maNguoiDung()), new ChiTietPhien(ho.maHoPhien())));
		}
	}

}
