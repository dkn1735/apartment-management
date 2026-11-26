package vn.edu.hust.chungcu.canho.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.canho.dto.CanHoResponse;
import vn.edu.hust.chungcu.canho.dto.SuaCanHoRequest;
import vn.edu.hust.chungcu.canho.repository.CanHoGhi;
import vn.edu.hust.chungcu.canho.repository.CanHoQuery;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Sửa diện tích và trạng thái sử dụng của một căn (spec 0012, AC-11 đến AC-14). Giá trị
 * cũ đọc dưới khóa dòng, nên mỗi dòng nhật ký ghi đúng giá trị ngay trước lần sửa đó.
 */
@Service
public class SuaCanHoService {

	private final CanHoQuery query;

	private final CanHoGhi ghi;

	private final NhatKyService nhatKy;

	private final NguoiDoiTienIch nguoiDoi;

	public SuaCanHoService(CanHoQuery query, CanHoGhi ghi, NhatKyService nhatKy, NguoiDoiTienIch nguoiDoi) {
		this.query = query;
		this.ghi = ghi;
		this.nhatKy = nhatKy;
		this.nguoiDoi = nguoiDoi;
	}

	/**
	 * Thứ tự kiểm: body (400), phạm vi (404), rồi đọc dưới khóa và cập nhật khi có đổi.
	 */
	@Transactional
	public CanHoResponse sua(PhamViTruyCap phamVi, String apartmentId, SuaCanHoRequest request) {
		CanHoGhi.GiaTri moi = kiem(request);
		String ma = NhapLieu.chuanHoaMaToaNha(apartmentId);
		if (this.query.maToaNhaCua(ma).filter(phamVi.maToaNha()::contains).isEmpty()) {
			throw khongThay();
		}
		CanHoGhi.GiaTri cu = this.ghi.khoaDong(ma).orElseThrow(SuaCanHoService::khongThay);
		ChiTietCanHo.Sua thayDoi = thayDoi(cu, moi);
		if (thayDoi != null) {
			this.ghi.capNhat(ma, moi.dienTich(), moi.trangThaiSuDung());
			this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.CANHO_SUA_CAN_HO, phamVi.maNguoiDung(),
					DoiTuongTacDong.canHo(ma), thayDoi));
		}
		return this.nguoiDoi.ganHoTen(this.query.cua(ma).orElseThrow(SuaCanHoService::khongThay));
	}

	/** Thứ tự mục lỗi của AC-12: dienTich, trangThaiSuDung. */
	static CanHoGhi.GiaTri kiem(SuaCanHoRequest request) {
		List<FieldError> loi = new ArrayList<>();
		BigDecimal dienTich = NhapLieuCanHo.dienTich(request.dienTich(), loi);
		String trangThai = null;
		if (NhapLieuCanHo.vang(request.trangThaiSuDung())) {
			loi.add(new FieldError("trangThaiSuDung", "Vui lòng chọn trạng thái sử dụng"));
		}
		else {
			trangThai = NhapLieuCanHo.trangThai(request.trangThaiSuDung(), loi);
		}
		if (!loi.isEmpty()) {
			throw CanHoLoi.duLieuKhongHopLe(loi);
		}
		return new CanHoGhi.GiaTri(dienTich, trangThai);
	}

	/**
	 * Chỉ trường đổi; diện tích so theo giá trị số ({@code 75.5} bằng {@code 75.50}).
	 * Không đổi gì thì {@code null}.
	 */
	static ChiTietCanHo.Sua thayDoi(CanHoGhi.GiaTri cu, CanHoGhi.GiaTri moi) {
		ChiTietCanHo.ThayDoi<BigDecimal> dienTich = (cu.dienTich().compareTo(moi.dienTich()) != 0)
				? new ChiTietCanHo.ThayDoi<>(cu.dienTich().setScale(2), moi.dienTich().setScale(2)) : null;
		ChiTietCanHo.ThayDoi<String> trangThai = !cu.trangThaiSuDung().equals(moi.trangThaiSuDung())
				? new ChiTietCanHo.ThayDoi<>(cu.trangThaiSuDung(), moi.trangThaiSuDung()) : null;
		return (dienTich == null && trangThai == null) ? null : new ChiTietCanHo.Sua(dienTich, trangThai);
	}

	private static ApiException khongThay() {
		return ApiException.notFound(PhamViTruyCap.KHONG_TIM_THAY);
	}

}
