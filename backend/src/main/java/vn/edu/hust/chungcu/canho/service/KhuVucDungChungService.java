package vn.edu.hust.chungcu.canho.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.canho.dto.KhuVucDungChungResponse;
import vn.edu.hust.chungcu.canho.repository.KhuVucDungChungQuery;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Danh mục khu vực dùng chung trong phạm vi người gọi (spec 0011, AC-11). Chỉ đọc; khu
 * vực đến từ seed.
 */
@Service
public class KhuVucDungChungService {

	public static final Set<String> TRUONG_SAP_XEP = KhuVucDungChungQuery.COT_SAP_XEP.keySet();

	public static final String SAP_XEP_MAC_DINH = "tenKhuVuc,asc";

	static final int TU_KHOA_TOI_DA = 100;

	private final KhuVucDungChungQuery query;

	public KhuVucDungChungService(KhuVucDungChungQuery query) {
		this.query = query;
	}

	/**
	 * Tham số sai (400) trước phạm vi (404). {@code maToaNha} rỗng coi như không gửi;
	 * {@code q} rỗng sau chuẩn hóa thì không lọc.
	 */
	@Transactional(readOnly = true)
	public PageResponse<KhuVucDungChungResponse> danhSach(PhamViTruyCap phamVi, String maToaNha, String q, String page,
			String size, String sort) {
		List<FieldError> loi = new ArrayList<>();
		TrangYeuCau trang = TrangYeuCau.tu(page, size, sort, TRUONG_SAP_XEP, SAP_XEP_MAC_DINH, loi);
		String tuKhoa = NhapLieu.chuanHoaChu(q);
		if (NhapLieu.doDai(tuKhoa) > TU_KHOA_TOI_DA) {
			loi.add(new FieldError("q", "Từ khóa tối đa 100 ký tự"));
		}
		if (!loi.isEmpty()) {
			throw CanHoLoi.duLieuKhongHopLe(loi);
		}
		List<String> toa = phamVi.maToaNha();
		if (maToaNha != null && !maToaNha.isEmpty()) {
			String ma = maToaNha.toUpperCase(Locale.ROOT);
			phamVi.yeuCauToaNha(ma);
			toa = List.of(ma);
		}
		if (toa.isEmpty()) {
			return PageResponse.of(List.of(), trang, 0);
		}
		String loc = tuKhoa.isEmpty() ? null : tuKhoa;
		long tong = this.query.dem(toa, loc);
		List<KhuVucDungChungResponse> items = (trang.offset() < tong) ? this.query.danhSach(toa, loc, trang)
				: List.of();
		return PageResponse.of(items, trang, tong);
	}

}
