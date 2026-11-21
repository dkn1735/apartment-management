package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.DoiTrangThaiRequest;
import vn.edu.hust.chungcu.auth.dto.TaiKhoanChiTietResponse;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanGhi;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanQuery;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Khóa, ngừng hoạt động và mở lại tài khoản (spec 0008, AC-10, AC-11). Khóa hay ngừng thì
 * thu hồi mọi họ phiên; mở lại thì đặt lại bộ đếm đăng nhập sai.
 */
@Service
public class TrangThaiTaiKhoanService {

	static final int LY_DO_TOI_DA = 500;

	private static final Logger log = LoggerFactory.getLogger(TrangThaiTaiKhoanService.class);

	private final TaiKhoanDichService dich;

	private final TaiKhoanPhamViService phamVi;

	private final TaiKhoanQuery query;

	private final TaiKhoanGhi ghi;

	private final ThuHoiPhienService thuHoiPhien;

	private final TaiKhoanTraCuuService traCuu;

	private final NhatKyService nhatKy;

	private final Clock clock;

	public TrangThaiTaiKhoanService(TaiKhoanDichService dich, TaiKhoanPhamViService phamVi, TaiKhoanQuery query,
			TaiKhoanGhi ghi, ThuHoiPhienService thuHoiPhien, TaiKhoanTraCuuService traCuu, NhatKyService nhatKy,
			Clock clock) {
		this.dich = dich;
		this.phamVi = phamVi;
		this.query = query;
		this.ghi = ghi;
		this.thuHoiPhien = thuHoiPhien;
		this.traCuu = traCuu;
		this.nhatKy = nhatKy;
		this.clock = clock;
	}

	@Transactional
	public TaiKhoanChiTietResponse doi(NguoiQuanLy nguoi, String maNguoiDung, DoiTrangThaiRequest request) {
		List<FieldError> loi = new ArrayList<>();
		String moi = request.trangThai();
		if (moi == null || !TrangThaiTaiKhoan.TAT_CA.contains(moi)) {
			loi.add(new FieldError("trangThai", "Trạng thái không tồn tại"));
		}
		String lyDo = TaiKhoanHopLe.chuanHoaChu(request.lyDo());
		boolean moLai = TrangThaiTaiKhoan.HOAT_DONG.equals(moi);
		if (lyDo.length() > LY_DO_TOI_DA) {
			loi.add(new FieldError("lyDo", "Lý do tối đa 500 ký tự"));
		}
		else if (lyDo.isEmpty() && moi != null && !moLai) {
			loi.add(new FieldError("lyDo", "Vui lòng nhập lý do"));
		}
		if (!loi.isEmpty()) {
			throw TaiKhoanLoi.duLieuKhongHopLe(loi);
		}
		// Khóa hay ngừng có thể làm giảm số Admin đang hoạt động: lấy khóa trước khi đọc
		// tài
		// khoản đích (AC-11)
		if (!moLai) {
			this.ghi.khoaAdmin();
		}
		TaiKhoanDichService.TaiKhoanDich tk = this.dich.khoa(nguoi, maNguoiDung);
		String cu = tk.dong().trangThaiTaiKhoan();
		this.phamVi.kiemDoiTrangThai(nguoi, cu);
		if (cu.equals(moi)) {
			return this.traCuu.timChiTiet(maNguoiDung);
		}
		if (!moLai) {
			if (maNguoiDung.equals(nguoi.maNguoiDung())) {
				throw TaiKhoanLoi.tuBaoVe();
			}
			if (tk.co(VaiTro.QUAN_TRI_VIEN) && tk.dangHoatDong() && this.query.demAdminHoatDongTru(maNguoiDung) < 1) {
				throw TaiKhoanLoi.adminCuoi();
			}
		}
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		// Ghi trạng thái trước rồi mới khóa từng họ phiên: lần làm mới chờ khóa họ sẽ
		// thấy họ
		// đã bị thu hồi
		this.ghi.capNhatTrangThai(maNguoiDung, moi, moLai, now);
		LoaiThaoTac loai;
		ChiTietNhatKy chiTiet;
		if (moLai) {
			loai = LoaiThaoTac.AUTH_MO_KHOA_TAI_KHOAN;
			chiTiet = new ChiTietTaiKhoan.MoKhoa(cu, moi, lyDo.isEmpty() ? null : lyDo);
		}
		else {
			int soPhien = this.thuHoiPhien.thuHoi(maNguoiDung, null, now, LyDoThuHoi.TAI_KHOAN_BI_KHOA);
			loai = LoaiThaoTac.AUTH_KHOA_TAI_KHOAN;
			chiTiet = new ChiTietTaiKhoan.Khoa(cu, moi, lyDo, soPhien);
		}
		log.info("{}: maNguoiThucHien={}, maNguoiDung={}, trangThaiCu={}, trangThaiMoi={}", loai, nguoi.maNguoiDung(),
				maNguoiDung, cu, moi);
		this.nhatKy.ghi(new SuKienNhatKy(loai, nguoi.maNguoiDung(), DoiTuongTacDong.nguoiDung(maNguoiDung), chiTiet));
		return this.traCuu.timChiTiet(maNguoiDung);
	}

}
