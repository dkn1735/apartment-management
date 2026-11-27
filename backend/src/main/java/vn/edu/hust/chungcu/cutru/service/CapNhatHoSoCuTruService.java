package vn.edu.hust.chungcu.cutru.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.cutru.dto.CapNhatHoSoCuTruRequest;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Sửa hoặc kết thúc một hồ sơ cư trú (spec 0014, AC-7, AC-8, AC-10, AC-12). Thứ tự: 403
 * dòng nhân viên BQL, 400 body, 404 phạm vi, khóa dòng căn rồi dòng hồ sơ, 409 trạng thái
 * hiện tại, ngắn mạch khi không đổi, 409 chồng ngày; rồi cập nhật, đồng bộ căn và ghi
 * nhật ký.
 */
@Service
public class CapNhatHoSoCuTruService {

	private final CuTruDinhDanhService dinhDanh;

	private final CanHoCuTruService canHo;

	private final HoSoCuTruQuery query;

	private final HoSoCuTruGhi ghi;

	private final KiemChongNgay chongNgay;

	private final DongBoTrangThaiCanHo dongBo;

	private final DungHoSoCuTru dungHoSo;

	private final NhatKyService nhatKy;

	private final Clock clock;

	public CapNhatHoSoCuTruService(CuTruDinhDanhService dinhDanh, CanHoCuTruService canHo, HoSoCuTruQuery query,
			HoSoCuTruGhi ghi, KiemChongNgay chongNgay, DongBoTrangThaiCanHo dongBo, DungHoSoCuTru dungHoSo,
			NhatKyService nhatKy, Clock clock) {
		this.dinhDanh = dinhDanh;
		this.canHo = canHo;
		this.query = query;
		this.ghi = ghi;
		this.chongNgay = chongNgay;
		this.dongBo = dongBo;
		this.dungHoSo = dungHoSo;
		this.nhatKy = nhatKy;
		this.clock = clock;
	}

	@Transactional
	public HoSoCuTruResponse capNhat(PhamViTruyCap phamVi, String recordId, CapNhatHoSoCuTruRequest request) {
		String maNv = this.dinhDanh.maNvbqlCua(phamVi.maNguoiDung()).orElseThrow(CuTruLoi::khongCoQuyen);
		LocalDate homNay = LocalDate.now(this.clock);
		HoSoCuTruGhi.GiaTri moi = kiem(request, homNay);
		String maHoSo = NhapLieuCuTru.maHoSo(recordId);
		String maCanHo = (maHoSo != null) ? this.query.cua(maHoSo).map(HoSoCuTruDong::maCanHo).orElse(null) : null;
		if (maCanHo == null || this.canHo.maToaNhaCua(maCanHo).filter(phamVi.maToaNha()::contains).isEmpty()) {
			throw CuTruLoi.khongThay();
		}
		String trangThaiCan = this.canHo.khoaVaDocTrangThai(maCanHo).orElseThrow(CuTruLoi::khongThay);
		HoSoCuTruDong cu = this.ghi.khoaDong(maHoSo).orElseThrow(CuTruLoi::khongThay);
		if (GiaTriCuTru.HET_HIEU_LUC.equals(cu.trangThaiHoSo())) {
			throw CuTruLoi.daHetHieuLuc();
		}
		if (GiaTriCuTru.TU_CHOI.equals(cu.trangThaiHoSo()) || GiaTriCuTru.DA_HUY.equals(cu.trangThaiHoSo())) {
			throw CuTruLoi.deNghiDaKetThuc();
		}
		if (!GiaTriCuTru.HOP_LE.equals(cu.trangThaiHoSo())) {
			throw CuTruLoi.dangChoXacMinh();
		}
		if (NhatKyCapNhatCuTru.khongDoi(cu, moi)) {
			return this.dungHoSo.dung(cu);
		}
		this.chongNgay.kiem(maCanHo, cu.maCuDan(), moi, maHoSo);
		try {
			this.ghi.capNhat(maHoSo, moi, maNv, Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS));
		}
		catch (DataIntegrityViolationException ex) {
			throw KiemChongNgay.dich(ex);
		}
		ChiTietCuTru.ThayDoi<String> doiCan = this.dongBo.dongBo(maCanHo, trangThaiCan, homNay);
		this.nhatKy.ghi(new SuKienNhatKy(NhatKyCapNhatCuTru.loai(moi), phamVi.maNguoiDung(),
				DoiTuongTacDong.hoSoCuTru(maHoSo), NhatKyCapNhatCuTru.chiTiet(cu, moi, doiCan)));
		return this.dungHoSo.dung(this.query.cua(maHoSo).orElseThrow());
	}

	/**
	 * Thứ tự mục lỗi của AC-8: vaiTroCuTru, tinhTrangCuTru, ngayBatDau, ngayKetThuc,
	 * trangThaiHoSo. Luật của ngày kết thúc theo trạng thái mới.
	 */
	static HoSoCuTruGhi.GiaTri kiem(CapNhatHoSoCuTruRequest request, LocalDate homNay) {
		List<FieldError> loi = new ArrayList<>();
		String vaiTro = NhapLieuCuTru.chon(request.vaiTroCuTru(), "vaiTroCuTru", GiaTriCuTru.VAI_TRO,
				"Vui lòng chọn vai trò cư trú", "Vai trò cư trú không hợp lệ", loi);
		String tinhTrang = NhapLieuCuTru.chon(request.tinhTrangCuTru(), "tinhTrangCuTru", GiaTriCuTru.TINH_TRANG,
				"Vui lòng chọn tình trạng cư trú", "Tình trạng cư trú không hợp lệ", loi);
		LocalDate batDau = NhapLieuCuTru.ngayBatDau(request.ngayBatDau(), loi);
		NhapLieuCuTru.NgayNhap ketThuc = NhapLieuCuTru.ngayKetThuc(request.ngayKetThuc());
		List<FieldError> loiTrangThai = new ArrayList<>();
		String trangThai = NhapLieuCuTru.chon(request.trangThaiHoSo(), "trangThaiHoSo", GiaTriCuTru.TRANG_THAI_GHI,
				"Vui lòng chọn trạng thái hồ sơ", "Trạng thái hồ sơ không hợp lệ", loiTrangThai);
		String loiKetThuc;
		if (GiaTriCuTru.HOP_LE.equals(trangThai)) {
			loiKetThuc = NhapLieuCuTru.loiKhiHopLe(ketThuc, batDau, tinhTrang, homNay);
		}
		else if (GiaTriCuTru.HET_HIEU_LUC.equals(trangThai)) {
			loiKetThuc = NhapLieuCuTru.loiKhiKetThuc(ketThuc, batDau, homNay);
		}
		else {
			loiKetThuc = ketThuc.sai() ? "Ngày kết thúc không hợp lệ" : null;
		}
		if (loiKetThuc != null) {
			loi.add(new FieldError(NhapLieuCuTru.NGAY_KET_THUC, loiKetThuc));
		}
		loi.addAll(loiTrangThai);
		if (!loi.isEmpty()) {
			throw CuTruLoi.duLieuKhongHopLe(loi);
		}
		return new HoSoCuTruGhi.GiaTri(vaiTro, tinhTrang, batDau, ketThuc.giaTri(), trangThai);
	}

}
