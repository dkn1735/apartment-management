package vn.edu.hust.chungcu.auth.service;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanGhi;
import vn.edu.hust.chungcu.auth.repository.VaiTroGhi;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Phần ghi DB của tạo tài khoản, trong một transaction: dòng người dùng và một dòng bảng
 * con mỗi vai trò (spec 0008, AC-1). Lỗi UNIQUE khi chạy đua đổi thành 409 (AC-3).
 */
@Service
public class GhiTaiKhoanMoiService {

	private static final Logger log = LoggerFactory.getLogger(GhiTaiKhoanMoiService.class);

	private final TaiKhoanGhi ghi;

	private final NhatKyService nhatKy;

	public GhiTaiKhoanMoiService(TaiKhoanGhi ghi, NhatKyService nhatKy) {
		this.ghi = ghi;
		this.nhatKy = nhatKy;
	}

	/**
	 * Ghi tài khoản và các vai trò, trả mã người dùng vừa tạo.
	 */
	@Transactional
	public String ghi(String maNguoiQuanLy, TaiKhoanHopLe.ThongTinChung thongTin, List<VaiTroGhi> vaiTro,
			String matKhauHash, Instant now) {
		String maNguoiDung;
		try {
			maNguoiDung = this.ghi.themNguoiDung(thongTin.hoTen(), thongTin.soDienThoai(), thongTin.email(),
					matKhauHash, now);
			for (VaiTroGhi v : vaiTro) {
				this.ghi.themVaiTro(maNguoiDung, v);
			}
		}
		catch (DataIntegrityViolationException ex) {
			throw TaiKhoanLoi.tuRangBuoc(ex);
		}
		log.info("Tạo tài khoản: maNguoiThucHien={}, maNguoiDung={}, vaiTro={}", maNguoiQuanLy, maNguoiDung,
				vaiTro.stream().map((v) -> v.vaiTro().name()).toList());
		// Đang trong transaction: dòng nhật ký chờ tới sau commit (spec 0007, AC-6)
		this.nhatKy
			.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_TAO_TAI_KHOAN, maNguoiQuanLy, DoiTuongTacDong.nguoiDung(maNguoiDung),
					new ChiTietTaiKhoan.Tao(vaiTro.stream().map(VaiTroGhi::vaiTro).toList())));
		return maNguoiDung;
	}

}
