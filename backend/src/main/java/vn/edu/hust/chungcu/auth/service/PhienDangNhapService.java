package vn.edu.hust.chungcu.auth.service;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.entity.PhienDangNhap;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.auth.repository.PhienDangNhapRepository;

/**
 * Phần ghi DB của một lần đăng nhập, trong một transaction (spec 0004, bước 7). Tách khỏi
 * {@link DangNhapService} để proxy transaction có tác dụng và bcrypt chạy ngoài
 * transaction.
 */
@Service
public class PhienDangNhapService {

	private final NguoiDungHeThongRepository nguoiDung;

	private final PhienDangNhapRepository phien;

	public PhienDangNhapService(NguoiDungHeThongRepository nguoiDung, PhienDangNhapRepository phien) {
		this.nguoiDung = nguoiDung;
		this.phien = phien;
	}

	@Transactional
	public void taoPhien(PhienMoi moi) {
		this.nguoiDung.ghiDangNhapCuoi(moi.maNguoiDung(), moi.taoLuc());
		if (moi.hashMoi() != null) {
			this.nguoiDung.nangCapMatKhauHash(moi.maNguoiDung(), moi.hashCu(), moi.hashMoi());
		}
		this.phien.save(new PhienDangNhap(moi.maNguoiDung(), moi.maHoPhien(), moi.tokenHash(), moi.taoLuc(),
				moi.hetHanLuc(), moi.diaChiIp(), moi.userAgent()));
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
