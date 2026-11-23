package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.CapNhatTaiKhoanRequest;
import vn.edu.hust.chungcu.auth.dto.TaiKhoanChiTietResponse;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanGhi;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanQuery;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanQuery.DongTaiKhoan;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Sửa họ tên, số điện thoại, email của một tài khoản (spec 0008, AC-7). Không thu hồi
 * phiên; lần đăng nhập sau dùng giá trị mới.
 */
@Service
public class CapNhatTaiKhoanService {

	private static final Logger log = LoggerFactory.getLogger(CapNhatTaiKhoanService.class);

	private final TaiKhoanDichService dich;

	private final TaiKhoanQuery query;

	private final TaiKhoanGhi ghi;

	private final TaiKhoanTraCuuService traCuu;

	private final NhatKyService nhatKy;

	private final Clock clock;

	private final KhoaTamDangNhapService khoaTam;

	public CapNhatTaiKhoanService(TaiKhoanDichService dich, TaiKhoanQuery query, TaiKhoanGhi ghi,
			TaiKhoanTraCuuService traCuu, NhatKyService nhatKy, Clock clock, KhoaTamDangNhapService khoaTam) {
		this.khoaTam = khoaTam;
		this.dich = dich;
		this.query = query;
		this.ghi = ghi;
		this.traCuu = traCuu;
		this.nhatKy = nhatKy;
		this.clock = clock;
	}

	@Transactional
	public TaiKhoanChiTietResponse capNhat(NguoiQuanLy nguoi, String maNguoiDung, CapNhatTaiKhoanRequest request) {
		List<FieldError> loi = new ArrayList<>();
		TaiKhoanHopLe.ThongTinChung moi = TaiKhoanHopLe.thongTinChung(request.hoTen(), request.soDienThoai(),
				request.email(), loi);
		TaiKhoanHopLe.kiemPhienBan(request.phienBan(), loi);
		if (!loi.isEmpty()) {
			throw TaiKhoanLoi.duLieuKhongHopLe(loi);
		}
		DongTaiKhoan cu = this.dich.khoa(nguoi, maNguoiDung).dong();
		if (cu.phienBan() != request.phienBan()) {
			throw TaiKhoanLoi.lechPhienBan();
		}
		List<String> doi = new ArrayList<>();
		if (!Objects.equals(cu.hoTen(), moi.hoTen())) {
			doi.add("hoTen");
		}
		if (!Objects.equals(cu.soDienThoai(), moi.soDienThoai())) {
			doi.add("soDienThoai");
		}
		if (!Objects.equals(cu.email(), moi.email())) {
			doi.add("email");
		}
		if (doi.isEmpty()) {
			return this.traCuu.timChiTiet(maNguoiDung);
		}
		List<String> trung = this.query.truongTrung(doi.contains("soDienThoai") ? moi.soDienThoai() : null,
				doi.contains("email") ? moi.email() : null, null, maNguoiDung);
		if (!trung.isEmpty()) {
			throw TaiKhoanLoi.trung(trung);
		}
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		try {
			this.ghi.capNhatChung(maNguoiDung, moi.hoTen(), moi.soDienThoai(), moi.email(), request.phienBan(), now);
		}
		catch (DataIntegrityViolationException ex) {
			throw TaiKhoanLoi.tuRangBuoc(ex);
		}
		// Số điện thoại, email mới không mang theo khóa của định danh ảo (spec 0009,
		// AC-19)
		this.khoaTam.xoaDinhDanh(doi.contains("soDienThoai") ? moi.soDienThoai() : null,
				doi.contains("email") ? moi.email() : null);
		log.info("Cập nhật tài khoản: maNguoiThucHien={}, maNguoiDung={}, truongThayDoi={}", nguoi.maNguoiDung(),
				maNguoiDung, doi);
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_CAP_NHAT_TAI_KHOAN, nguoi.maNguoiDung(),
				DoiTuongTacDong.nguoiDung(maNguoiDung), new ChiTietTaiKhoan.CapNhat(doi)));
		return this.traCuu.timChiTiet(maNguoiDung);
	}

}
