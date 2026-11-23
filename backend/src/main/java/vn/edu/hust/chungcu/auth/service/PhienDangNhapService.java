package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.entity.PhienDangNhap;
import vn.edu.hust.chungcu.auth.repository.KhoaTamTaiKhoanRepository;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.auth.repository.PhienDangNhapRepository;
import vn.edu.hust.chungcu.common.config.AppProperties;

/**
 * Phần ghi DB của một lần đăng nhập, trong một transaction (spec 0004, bước 7). Tách khỏi
 * {@link DangNhapService} để proxy transaction có tác dụng và bcrypt chạy ngoài
 * transaction. Cũng dọn các dòng phiên đã hết hạn hoặc thu hồi lâu (spec 0005, AC-15).
 */
@Service
public class PhienDangNhapService {

	private final NguoiDungHeThongRepository nguoiDung;

	private final PhienDangNhapRepository phien;

	private final Clock clock;

	private final Duration retention;

	private final KhoaTamTaiKhoanRepository khoaTam;

	public PhienDangNhapService(NguoiDungHeThongRepository nguoiDung, PhienDangNhapRepository phien, Clock clock,
			AppProperties properties, KhoaTamTaiKhoanRepository khoaTam) {
		this.nguoiDung = nguoiDung;
		this.khoaTam = khoaTam;
		this.phien = phien;
		this.clock = clock;
		this.retention = properties.security().sessionRetention();
	}

	/**
	 * Xóa dòng có {@code het_han_luc} hoặc {@code thu_hoi_luc} trước giờ hiện tại trừ
	 * {@code session-retention}; trả số dòng đã xóa.
	 */
	@Transactional
	public int donDep() {
		return this.phien.xoaCu(Instant.now(this.clock).minus(this.retention));
	}

	@Transactional
	public void taoPhien(PhienMoi moi) {
		this.nguoiDung.ghiDangNhapCuoi(moi.maNguoiDung(), moi.taoLuc());
		// Mật khẩu khớp: bộ đếm về 0 và xóa khóa, rollback cùng phiên (spec 0009, AC-4).
		// JdbcClient dùng chung kết nối của JpaTransactionManager
		this.khoaTam.datLai(moi.maNguoiDung());
		if (moi.hashMoi() != null) {
			this.nguoiDung.nangCapMatKhauHash(moi.maNguoiDung(), moi.hashCu(), moi.hashMoi());
		}
		// Dòng đầu của họ phiên: bat_dau_luc = tao_luc (spec 0005, AC-9)
		this.phien.save(new PhienDangNhap(moi.maNguoiDung(), moi.maHoPhien(), moi.tokenHash(), moi.taoLuc(),
				moi.taoLuc(), moi.hetHanLuc(), moi.diaChiIp(), moi.userAgent()));
	}

	/**
	 * Dữ liệu của một phiên mới.
	 *
	 * @param hashCu hash đang có trong DB, dùng làm điều kiện khi nâng hash
	 * @param hashMoi hash cost mới, {@code null} khi không cần nâng
	 */
	public record PhienMoi(String maNguoiDung, UUID maHoPhien, String tokenHash, Instant taoLuc, Instant hetHanLuc,
			String diaChiIp, String userAgent, String hashCu, String hashMoi) {

		@Override
		public String toString() {
			return "PhienMoi[maNguoiDung=" + this.maNguoiDung + ", maHoPhien=" + this.maHoPhien + "]";
		}

	}

}
