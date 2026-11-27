package vn.edu.hust.chungcu.cutru.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.auth.service.TrangThaiTaiKhoan;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.dto.ThemHoSoCuTruRequest;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Ghi nhận hồ sơ cư trú mới (spec 0014, AC-4 đến AC-6, AC-10, AC-12). Thứ tự: 403 dòng
 * nhân viên BQL, 400 body, 404 phạm vi căn, khóa dòng căn, 404/422 cư dân, 409 chồng
 * ngày; rồi chèn, đồng bộ trạng thái căn và ghi nhật ký trong cùng transaction.
 */
@Service
public class ThemHoSoCuTruService {

	private final CuTruDinhDanhService dinhDanh;

	private final CanHoCuTruService canHo;

	private final HoSoCuTruQuery query;

	private final HoSoCuTruGhi ghi;

	private final KiemChongNgay chongNgay;

	private final DongBoTrangThaiCanHo dongBo;

	private final DungHoSoCuTru dungHoSo;

	private final NhatKyService nhatKy;

	private final Clock clock;

	public ThemHoSoCuTruService(CuTruDinhDanhService dinhDanh, CanHoCuTruService canHo, HoSoCuTruQuery query,
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
	public HoSoCuTruResponse them(PhamViTruyCap phamVi, ThemHoSoCuTruRequest request) {
		String maNv = this.dinhDanh.maNvbqlCua(phamVi.maNguoiDung()).orElseThrow(CuTruLoi::khongCoQuyen);
		LocalDate homNay = LocalDate.now(this.clock);
		HoSoMoi moi = kiem(request, homNay);
		String ma = moi.maCanHo();
		if (this.canHo.maToaNhaCua(ma).filter(phamVi.maToaNha()::contains).isEmpty()) {
			throw CuTruLoi.khongThay();
		}
		String trangThaiCan = this.canHo.khoaVaDocTrangThai(ma).orElseThrow(CuTruLoi::khongThay);
		ThongTinCuDan cuDan = this.dinhDanh.chiTietCuDanTheoMa(List.of(moi.maCuDan())).get(moi.maCuDan());
		if (cuDan == null) {
			throw CuTruLoi.khongThayCuDan();
		}
		if (TrangThaiTaiKhoan.NGUNG_HOAT_DONG.equals(cuDan.trangThaiTaiKhoan())) {
			throw CuTruLoi.taiKhoanNgungHoatDong();
		}
		this.chongNgay.kiem(ma, moi.maCuDan(), moi.giaTri(), null);
		Instant luc = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		String maHoSo;
		try {
			maHoSo = this.ghi.them(ma, moi.maCuDan(), maNv, moi.giaTri(), luc);
		}
		catch (DataIntegrityViolationException ex) {
			throw KiemChongNgay.dich(ex);
		}
		ChiTietCuTru.ThayDoi<String> doiCan = this.dongBo.dongBo(ma, trangThaiCan, homNay);
		HoSoCuTruGhi.GiaTri g = moi.giaTri();
		this.nhatKy
			.ghi(new SuKienNhatKy(LoaiThaoTac.CUTRU_THEM_HO_SO, phamVi.maNguoiDung(), DoiTuongTacDong.hoSoCuTru(maHoSo),
					new ChiTietCuTru.Them(ma, moi.maCuDan(), g.vaiTroCuTru(), g.tinhTrangCuTru(),
							g.ngayBatDau().toString(), Objects.toString(g.ngayKetThuc(), null), doiCan)));
		return this.dungHoSo.dung(this.query.cua(maHoSo).orElseThrow());
	}

	/**
	 * Thứ tự mục lỗi của AC-5: maCanHo, maCuDan, vaiTroCuTru, tinhTrangCuTru, ngayBatDau,
	 * ngayKetThuc.
	 */
	static HoSoMoi kiem(ThemHoSoCuTruRequest request, LocalDate homNay) {
		List<FieldError> loi = new ArrayList<>();
		String maCanHo = NhapLieuCuTru.maCanHo(request.maCanHo(), loi);
		String maCuDan = NhapLieuCuTru.maCuDan(request.maCuDan(), loi);
		String vaiTro = NhapLieuCuTru.chon(request.vaiTroCuTru(), "vaiTroCuTru", GiaTriCuTru.VAI_TRO,
				"Vui lòng chọn vai trò cư trú", "Vai trò cư trú không hợp lệ", loi);
		String tinhTrang = NhapLieuCuTru.chon(request.tinhTrangCuTru(), "tinhTrangCuTru", GiaTriCuTru.TINH_TRANG,
				"Vui lòng chọn tình trạng cư trú", "Tình trạng cư trú không hợp lệ", loi);
		LocalDate batDau = NhapLieuCuTru.ngayBatDau(request.ngayBatDau(), loi);
		NhapLieuCuTru.NgayNhap ketThuc = NhapLieuCuTru.ngayKetThuc(request.ngayKetThuc());
		String loiKetThuc = NhapLieuCuTru.loiKhiHopLe(ketThuc, batDau, tinhTrang, homNay);
		if (loiKetThuc != null) {
			loi.add(new FieldError(NhapLieuCuTru.NGAY_KET_THUC, loiKetThuc));
		}
		if (!loi.isEmpty()) {
			throw CuTruLoi.duLieuKhongHopLe(loi);
		}
		return new HoSoMoi(maCanHo, maCuDan,
				new HoSoCuTruGhi.GiaTri(vaiTro, tinhTrang, batDau, ketThuc.giaTri(), GiaTriCuTru.HOP_LE));
	}

	/** Hồ sơ mới đã chuẩn hóa và hợp lệ. */
	record HoSoMoi(String maCanHo, String maCuDan, HoSoCuTruGhi.GiaTri giaTri) {
	}

}
