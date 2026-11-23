package vn.edu.hust.chungcu.auth.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanGhi;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Phần ghi DB của đặt lại mật khẩu, trong một transaction: khóa dòng đích và kiểm phạm
 * vi, thay hash, bật cờ bắt đổi, thu hồi mọi họ phiên (spec 0008, AC-12).
 */
@Service
public class GhiDatLaiMatKhauService {

	private static final Logger log = LoggerFactory.getLogger(GhiDatLaiMatKhauService.class);

	private final TaiKhoanDichService dich;

	private final TaiKhoanGhi ghi;

	private final ThuHoiPhienService thuHoiPhien;

	private final NhatKyService nhatKy;

	private final HuyYeuCauDatLaiService huyYeuCauDatLai;

	public GhiDatLaiMatKhauService(TaiKhoanDichService dich, TaiKhoanGhi ghi, ThuHoiPhienService thuHoiPhien,
			NhatKyService nhatKy, HuyYeuCauDatLaiService huyYeuCauDatLai) {
		this.huyYeuCauDatLai = huyYeuCauDatLai;
		this.dich = dich;
		this.ghi = ghi;
		this.thuHoiPhien = thuHoiPhien;
		this.nhatKy = nhatKy;
	}

	@Transactional
	public void ghi(NguoiQuanLy nguoi, String maNguoiDung, String matKhauHash, Instant now) {
		this.dich.khoa(nguoi, maNguoiDung);
		this.ghi.datLaiMatKhau(maNguoiDung, matKhauHash, now);
		this.huyYeuCauDatLai.huyConMo(maNguoiDung, now);
		int soPhien = this.thuHoiPhien.thuHoi(maNguoiDung, null, now, LyDoThuHoi.DAT_LAI_MAT_KHAU);
		log.info("Đặt lại mật khẩu: maNguoiThucHien={}, maNguoiDung={}, soPhienThuHoi={}", nguoi.maNguoiDung(),
				maNguoiDung, soPhien);
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_DAT_LAI_MAT_KHAU, nguoi.maNguoiDung(),
				DoiTuongTacDong.nguoiDung(maNguoiDung), new ChiTietTaiKhoan.DatLai(soPhien)));
	}

}
