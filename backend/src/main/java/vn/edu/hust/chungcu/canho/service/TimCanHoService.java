package vn.edu.hust.chungcu.canho.service;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_TRI;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.CU_DAN;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.KE_TOAN;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.canho.dto.BoLocCanHo;
import vn.edu.hust.chungcu.canho.dto.CanHoResponse;
import vn.edu.hust.chungcu.canho.repository.CanHoQuery;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Tìm căn hộ có lọc và xem chi tiết một căn (spec 0012, AC-1 đến AC-6). Tham số sai (400)
 * trước phạm vi (404), theo thứ tự kiểm của spec 0006.
 */
@Service
public class TimCanHoService {

	public static final Set<String> TRUONG_SAP_XEP = CanHoQuery.COT_SAP_XEP.keySet();

	public static final String SAP_XEP_MAC_DINH = "maCanHo,asc";

	static final int TU_KHOA_TOI_DA = 20;

	private static final Pattern SO_TANG = Pattern.compile("^[1-9][0-9]?$");

	private final CanHoQuery query;

	private final NguoiDoiTienIch nguoiDoi;

	public TimCanHoService(CanHoQuery query, NguoiDoiTienIch nguoiDoi) {
		this.query = query;
		this.nguoiDoi = nguoiDoi;
	}

	@Transactional(readOnly = true)
	public PageResponse<CanHoResponse> timKiem(BoLocCanHo boLoc, String page, String size, String sort,
			PhamViTruyCap phamVi) {
		List<FieldError> loi = new ArrayList<>();
		TrangYeuCau trang = TrangYeuCau.tu(page, size, sort, TRUONG_SAP_XEP, SAP_XEP_MAC_DINH, loi);
		BoLocDaKiem loc = kiem(boLoc, loi);
		if (!loi.isEmpty()) {
			throw CanHoLoi.duLieuKhongHopLe(loi);
		}
		List<String> toa = phamVi.maToaNha();
		if (loc.maToaNha() != null) {
			phamVi.yeuCauToaNha(loc.maToaNha());
			toa = List.of(loc.maToaNha());
		}
		// Phạm vi rỗng: không chạy SQL, để không sinh IN ()
		if (toa.isEmpty()) {
			return PageResponse.of(List.of(), trang, 0);
		}
		CanHoQuery.DieuKien dk = new CanHoQuery.DieuKien(toa, loc.soTang(), loc.dienTichTu(), loc.dienTichDen(),
				loc.trangThai(), loc.q(), loc.biTamDungTienIch());
		long tong = this.query.dem(dk);
		List<CanHoResponse> items = (trang.offset() < tong) ? this.nguoiDoi.ganHoTen(this.query.tim(dk, trang))
				: List.of();
		return PageResponse.of(items, trang, tong);
	}

	/**
	 * Xem được khi có vai trò nhân viên đọc căn hộ và căn thuộc tòa trong phạm vi, hoặc
	 * có {@code CU_DAN} và căn nằm trong phạm vi căn (AC-6). Mọi trường hợp khác là 404.
	 * Người chỉ xem được nhờ điều kiện cư dân không thấy người đổi tiện ích (spec 0013,
	 * AC-9).
	 */
	@Transactional(readOnly = true)
	public CanHoResponse chiTiet(String apartmentId, PhamViTruyCap phamVi) {
		String ma = NhapLieu.chuanHoaMaToaNha(apartmentId);
		CanHoResponse canHo = this.query.cua(ma).orElseThrow(TimCanHoService::khongThay);
		boolean laCuDanCuaCan = phamVi.coVaiTro(CU_DAN) && phamVi.maCanHo().contains(ma);
		boolean laNhanVienCuaToa = (phamVi.coVaiTro(BAN_QUAN_LY) || phamVi.coVaiTro(BAN_QUAN_TRI)
				|| phamVi.coVaiTro(KE_TOAN)) && phamVi.maToaNha().contains(canHo.maToaNha());
		if (!laCuDanCuaCan && !laNhanVienCuaToa) {
			throw khongThay();
		}
		return laNhanVienCuaToa ? this.nguoiDoi.ganHoTen(canHo) : this.nguoiDoi.anNguoiDoi(canHo);
	}

	/**
	 * Kiểm theo AC-2, AC-3; lỗi theo thứ tự soTang, dienTichTu, dienTichDen, trạng thái,
	 * q, biTamDungTienIch (spec 0013, AC-11).
	 */
	static BoLocDaKiem kiem(BoLocCanHo boLoc, List<FieldError> loi) {
		String maToaNha = NhapLieuCanHo.gon(NhapLieu.chuanHoaMaToaNha(boLoc.maToaNha()));
		Integer soTang = null;
		String soTangNhap = NhapLieuCanHo.gon(boLoc.soTang());
		if (soTangNhap != null) {
			if (!SO_TANG.matcher(soTangNhap).matches()) {
				loi.add(new FieldError("soTang", NhapLieu.SO_TANG_SAI));
			}
			else if (maToaNha == null) {
				loi.add(new FieldError("soTang", "Chọn tòa nhà trước khi lọc theo tầng"));
			}
			else {
				soTang = Integer.valueOf(soTangNhap);
			}
		}
		BigDecimal tu = NhapLieuCanHo.dienTichLoc(boLoc.dienTichTu(), "dienTichTu", loi);
		BigDecimal den = NhapLieuCanHo.dienTichLoc(boLoc.dienTichDen(), "dienTichDen", loi);
		if (tu != null && den != null && tu.compareTo(den) > 0) {
			loi.add(new FieldError("dienTichDen", "Diện tích đến phải lớn hơn hoặc bằng diện tích từ"));
		}
		List<String> trangThai = trangThai(boLoc.trangThaiSuDung(), loi);
		String q = NhapLieuCanHo.gon(NhapLieu.chuanHoaMaToaNha(boLoc.q()));
		if (q != null && NhapLieu.doDai(q) > TU_KHOA_TOI_DA) {
			loi.add(new FieldError("q", "Từ khóa tìm kiếm tối đa 20 ký tự"));
		}
		Boolean tamDung = NhapLieuTienIch.boLoc(boLoc.biTamDungTienIch(), loi);
		return new BoLocDaKiem(maToaNha, soTang, tu, den, trangThai, q, tamDung);
	}

	/** Bỏ giá trị rỗng và giá trị lặp; một giá trị sai thì một mục lỗi. */
	private static List<String> trangThai(List<String> raw, List<FieldError> loi) {
		Set<String> ketQua = new LinkedHashSet<>();
		boolean sai = false;
		for (String giaTri : (raw != null) ? raw : List.<String>of()) {
			String gon = NhapLieuCanHo.gon(giaTri);
			if (gon == null) {
				continue;
			}
			String nfc = Normalizer.normalize(gon, Normalizer.Form.NFC);
			if (TrangThaiSuDung.hopLe(nfc)) {
				ketQua.add(nfc);
			}
			else {
				sai = true;
			}
		}
		if (sai) {
			loi.add(new FieldError("trangThaiSuDung", NhapLieuCanHo.TRANG_THAI_SAI));
		}
		return List.copyOf(ketQua);
	}

	private static ApiException khongThay() {
		return ApiException.notFound(PhamViTruyCap.KHONG_TIM_THAY);
	}

	/** Bộ lọc đã chuẩn hóa và hợp lệ; {@code null} là không lọc. */
	record BoLocDaKiem(String maToaNha, Integer soTang, BigDecimal dienTichTu, BigDecimal dienTichDen,
			List<String> trangThai, String q, Boolean biTamDungTienIch) {
	}

}
