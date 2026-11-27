package vn.edu.hust.chungcu.canho.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.canho.dto.CanHoResponse;
import vn.edu.hust.chungcu.canho.dto.ThemCanHoRequest;
import vn.edu.hust.chungcu.canho.repository.CanHoGhi;
import vn.edu.hust.chungcu.canho.repository.CanHoQuery;
import vn.edu.hust.chungcu.canho.repository.TangQuery;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.common.error.LoiCoSoDuLieu;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Thêm một căn vào một tầng có sẵn, mã căn do server ghép (spec 0012, AC-7 đến AC-9,
 * AC-14). Căn mới không được ở {@code Đang sử dụng} (spec 0014, AC-10). Kiểm trùng trước
 * để trường hợp thường gặp trả 409 ngay; hai request chạy đua thì khóa chính chặn và được
 * dịch về cùng 409.
 */
@Service
public class ThemCanHoService {

	static final String TANG_KHONG_TON_TAI = "Tầng không tồn tại";

	private static final String KHOA_CHINH = "can_ho_pkey";

	private final TangQuery tangQuery;

	private final CanHoQuery query;

	private final CanHoGhi ghi;

	private final NhatKyService nhatKy;

	private final NguoiDoiTienIch nguoiDoi;

	public ThemCanHoService(TangQuery tangQuery, CanHoQuery query, CanHoGhi ghi, NhatKyService nhatKy,
			NguoiDoiTienIch nguoiDoi) {
		this.tangQuery = tangQuery;
		this.query = query;
		this.ghi = ghi;
		this.nhatKy = nhatKy;
		this.nguoiDoi = nguoiDoi;
	}

	@Transactional
	public CanHoResponse them(PhamViTruyCap phamVi, ThemCanHoRequest request) {
		CanHoMoi moi = kiem(phamVi, request);
		// Căn mới chưa có hồ sơ cư trú nào (spec 0014, AC-10)
		if (TrangThaiSuDung.DANG_SU_DUNG.equals(moi.trangThai())) {
			throw CanHoLoi.trangThaiMauThuanCuTru(CanHoLoi.CHUA_CO_NGUOI_CU_TRU);
		}
		String ma = MaCanHo.cua(moi.maTang(), moi.soCan());
		if (this.query.tonTai(ma)) {
			throw CanHoLoi.trungCanHo(moi.soCan(), ma);
		}
		try {
			this.ghi.them(ma, moi.maTang(), moi.dienTich(), moi.trangThai());
		}
		catch (DuplicateKeyException ex) {
			// Transaction đã bị PostgreSQL hủy: chỉ ném lỗi, không chạy thêm SQL
			if (LoiCoSoDuLieu.tenRangBuoc(ex).filter(KHOA_CHINH::equals).isPresent()) {
				throw CanHoLoi.trungCanHo(moi.soCan(), ma);
			}
			throw ex;
		}
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.CANHO_THEM_CAN_HO, phamVi.maNguoiDung(), DoiTuongTacDong.canHo(ma),
				new ChiTietCanHo.Them(moi.maTang(), moi.dienTich(), moi.trangThai())));
		return this.nguoiDoi.ganHoTen(this.query.cua(ma).orElseThrow());
	}

	/** Kiểm theo thứ tự trường của AC-8: maTang, soCan, dienTich, trangThaiSuDung. */
	CanHoMoi kiem(PhamViTruyCap phamVi, ThemCanHoRequest request) {
		List<FieldError> loi = new ArrayList<>();
		String maTang = maTang(request.maTang(), phamVi, loi);
		Integer soCan = NhapLieuCanHo.soCan(request.soCan(), loi);
		BigDecimal dienTich = NhapLieuCanHo.dienTich(request.dienTich(), loi);
		String trangThai = NhapLieuCanHo.vang(request.trangThaiSuDung()) ? TrangThaiSuDung.TRONG
				: NhapLieuCanHo.trangThai(request.trangThaiSuDung(), loi);
		if (!loi.isEmpty()) {
			throw CanHoLoi.duLieuKhongHopLe(loi);
		}
		return new CanHoMoi(maTang, soCan, dienTich, trangThai);
	}

	/** Tầng phải có và thuộc tòa trong phạm vi tòa của người gọi. */
	private String maTang(JsonNode raw, PhamViTruyCap phamVi, List<FieldError> loi) {
		if (NhapLieuCanHo.vang(raw)) {
			loi.add(new FieldError("maTang", "Vui lòng chọn tầng"));
			return null;
		}
		if (!raw.isString()) {
			loi.add(new FieldError("maTang", TANG_KHONG_TON_TAI));
			return null;
		}
		String ma = NhapLieu.chuanHoaMaToaNha(raw.stringValue());
		if (ma.isEmpty()) {
			loi.add(new FieldError("maTang", "Vui lòng chọn tầng"));
			return null;
		}
		boolean trongPhamVi = this.tangQuery.maToaNhaCua(ma).filter(phamVi.maToaNha()::contains).isPresent();
		if (!trongPhamVi) {
			loi.add(new FieldError("maTang", TANG_KHONG_TON_TAI));
			return null;
		}
		return ma;
	}

	/** Dữ liệu căn đã chuẩn hóa và hợp lệ. */
	record CanHoMoi(String maTang, int soCan, BigDecimal dienTich, String trangThai) {
	}

}
