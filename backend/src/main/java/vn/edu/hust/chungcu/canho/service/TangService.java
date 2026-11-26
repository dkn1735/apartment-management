package vn.edu.hust.chungcu.canho.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.canho.dto.TangResponse;
import vn.edu.hust.chungcu.canho.dto.ThemTangRequest;
import vn.edu.hust.chungcu.canho.repository.TangQuery;
import vn.edu.hust.chungcu.canho.repository.ToaNhaGhi;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Xem và thêm tầng của một tòa (spec 0011, AC-4, AC-5, AC-8). Thêm tầng đọc số tầng dưới
 * khóa dòng tòa, nên hai BQL cùng thêm một tầng thì người sau nhận 422.
 */
@Service
public class TangService {

	public static final Set<String> TRUONG_SAP_XEP = TangQuery.COT_SAP_XEP.keySet();

	public static final String SAP_XEP_MAC_DINH = "soTang,asc";

	private final TangQuery query;

	private final ToaNhaGhi ghi;

	private final NhatKyService nhatKy;

	public TangService(TangQuery query, ToaNhaGhi ghi, NhatKyService nhatKy) {
		this.query = query;
		this.ghi = ghi;
		this.nhatKy = nhatKy;
	}

	/** Tầng của một tòa đã nằm trong phạm vi người gọi. */
	@Transactional(readOnly = true)
	public PageResponse<TangResponse> danhSach(String maToaNha, TrangYeuCau trang) {
		long tong = this.query.dem(maToaNha);
		List<TangResponse> items = (trang.offset() < tong) ? this.query.danhSach(maToaNha, trang) : List.of();
		return PageResponse.of(items, trang, tong);
	}

	/**
	 * Thứ tự kiểm của AC-5: {@code soTang} 1 đến 99 (400), phạm vi (404), rồi dưới khóa:
	 * trần 99 tầng, rồi tầng kế tiếp (422).
	 * @param maToaNha mã tòa đã đổi chữ hoa
	 */
	@Transactional
	public TangResponse them(PhamViTruyCap phamVi, String maToaNha, ThemTangRequest request) {
		List<FieldError> loi = new ArrayList<>();
		Integer soTang = NhapLieu.soTang(request.soTang(), loi);
		if (!loi.isEmpty()) {
			throw CanHoLoi.duLieuKhongHopLe(loi);
		}
		phamVi.yeuCauToaNha(maToaNha);
		int hienCo = this.ghi.soTangKhoaDong(maToaNha)
			.orElseThrow(() -> ApiException.notFound(PhamViTruyCap.KHONG_TIM_THAY));
		if (hienCo >= NhapLieu.SO_TANG_TOI_DA) {
			throw CanHoLoi.saiThuTu(CanHoLoi.TOI_DA_TANG);
		}
		if (soTang != hienCo + 1) {
			throw CanHoLoi.saiThuTu("Tầng tiếp theo của tòa này là tầng " + (hienCo + 1));
		}
		String maTang = MaTang.cua(maToaNha, soTang);
		this.ghi.themTang(maToaNha, soTang, maTang);
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.CANHO_THEM_TANG, phamVi.maNguoiDung(),
				DoiTuongTacDong.tang(maTang), new ChiTietToaNha.ThemTang(maToaNha, soTang)));
		return new TangResponse(maTang, soTang, 0);
	}

}
