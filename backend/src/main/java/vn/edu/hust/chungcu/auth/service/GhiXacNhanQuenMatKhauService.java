package vn.edu.hust.chungcu.auth.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanGhi;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Transaction của đặt lại mật khẩu qua email (spec 0010, Luồng xác nhận bước 4): khóa
 * dòng tài khoản, tiêu token, đổi hash, xóa cờ và khóa tạm, thu hồi mọi họ phiên. Mọi lỗi
 * ném ra làm rollback, nên token chỉ bị tiêu khi mật khẩu thật sự đổi.
 */
@Service
public class GhiXacNhanQuenMatKhauService {

	private static final Logger log = LoggerFactory.getLogger(GhiXacNhanQuenMatKhauService.class);

	private final QuenMatKhauRepository repository;

	private final TaiKhoanGhi taiKhoanGhi;

	private final ThuHoiPhienService thuHoiPhien;

	private final NhatKyService nhatKy;

	public GhiXacNhanQuenMatKhauService(QuenMatKhauRepository repository, TaiKhoanGhi taiKhoanGhi,
			ThuHoiPhienService thuHoiPhien, NhatKyService nhatKy) {
		this.repository = repository;
		this.taiKhoanGhi = taiKhoanGhi;
		this.thuHoiPhien = thuHoiPhien;
		this.nhatKy = nhatKy;
	}

	/**
	 * @return số họ phiên đã thu hồi
	 */
	@Transactional
	public int ghi(String maNguoiDung, String tokenHash, String matKhauHash, Instant now) {
		String trangThai = this.repository.khoaTaiKhoan(maNguoiDung).orElse(null);
		if (!TrangThaiTaiKhoan.HOAT_DONG.equals(trangThai)) {
			throw XacNhanQuenMatKhauService.tokenKhongHopLe();
		}
		// Chỗ chặn hai lần xác nhận song song và mọi lần hủy xen giữa lúc tra và lúc ghi
		// (AC-8)
		if (this.repository.tieuToken(tokenHash, maNguoiDung, now) == 0) {
			throw XacNhanQuenMatKhauService.tokenKhongHopLe();
		}
		this.taiKhoanGhi.datMatKhauQuaEmail(maNguoiDung, matKhauHash, now);
		int soHo = this.thuHoiPhien.thuHoi(maNguoiDung, null, now, LyDoThuHoi.DAT_LAI_MAT_KHAU);
		log.info("Đặt lại mật khẩu qua email: maNguoiDung={}, soPhienThuHoi={}", maNguoiDung, soHo);
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_DAT_LAI_MAT_KHAU_QUA_EMAIL, maNguoiDung,
				DoiTuongTacDong.nguoiDung(maNguoiDung), new ChiTietDatLaiQuaEmail(soHo)));
		return soHo;
	}

}
