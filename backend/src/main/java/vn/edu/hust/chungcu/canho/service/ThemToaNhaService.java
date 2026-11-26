package vn.edu.hust.chungcu.canho.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.canho.dto.ThemToaNhaRequest;
import vn.edu.hust.chungcu.canho.dto.ToaNhaResponse;
import vn.edu.hust.chungcu.canho.repository.ToaNhaGhi;
import vn.edu.hust.chungcu.canho.repository.ToaNhaQuery;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.common.error.LoiCoSoDuLieu;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Thêm tòa nhà cùng đủ tầng của nó trong một transaction (spec 0011, AC-1, AC-2, AC-3,
 * AC-14). Không cần khóa: khóa chính và unique index chặn trùng khi hai request chạy đua.
 */
@Service
public class ThemToaNhaService {

	static final int TEN_TOI_DA = 100;

	static final int DIA_CHI_TOI_DA = 255;

	private static final Pattern MA_TOA_NHA = Pattern.compile("^[A-Z0-9]{1,10}$");

	/** Tên ràng buộc trùng và trường tương ứng (V2, V15). */
	private static final Map<String, String> RANG_BUOC_TRUNG = Map.of("toa_nha_pkey", "maToaNha",
			"uq_toa_nha_ten_lower", "tenToaNha");

	private final ToaNhaQuery query;

	private final ToaNhaGhi ghi;

	private final NhatKyService nhatKy;

	public ThemToaNhaService(ToaNhaQuery query, ToaNhaGhi ghi, NhatKyService nhatKy) {
		this.query = query;
		this.ghi = ghi;
		this.nhatKy = nhatKy;
	}

	@Transactional
	public ToaNhaResponse them(String maNguoiThucHien, ThemToaNhaRequest request) {
		ToaNhaMoi moi = kiem(request);
		ToaNhaQuery.Trung trung = this.query.trung(moi.ma(), moi.ten());
		if (trung.ma() || trung.ten()) {
			throw CanHoLoi.trung(trung.ma(), trung.ten());
		}
		try {
			this.ghi.themToaNha(moi.ma(), moi.ten(), moi.soTang(), moi.diaChi());
		}
		catch (DataIntegrityViolationException ex) {
			throw tuRangBuoc(ex);
		}
		this.ghi.themTangTu1Den(moi.ma(), moi.soTang());
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.CANHO_THEM_TOA_NHA, maNguoiThucHien,
				DoiTuongTacDong.toaNha(moi.ma()), new ChiTietToaNha.Them(moi.soTang())));
		return new ToaNhaResponse(moi.ma(), moi.ten(), moi.soTang(), moi.diaChi(), 0);
	}

	/** Chuẩn hóa rồi kiểm theo thứ tự trường cố định của AC-2. */
	static ToaNhaMoi kiem(ThemToaNhaRequest request) {
		List<FieldError> loi = new ArrayList<>();
		String ma = NhapLieu.chuanHoaMaToaNha(request.maToaNha());
		if (ma.isEmpty()) {
			loi.add(new FieldError("maToaNha", "Vui lòng nhập mã tòa nhà"));
		}
		else if (!MA_TOA_NHA.matcher(ma).matches()) {
			loi.add(new FieldError("maToaNha", "Mã tòa nhà gồm 1 đến 10 chữ cái không dấu hoặc chữ số"));
		}
		String ten = NhapLieu.chuanHoaChu(request.tenToaNha());
		if (ten.isEmpty()) {
			loi.add(new FieldError("tenToaNha", "Vui lòng nhập tên tòa nhà"));
		}
		else if (NhapLieu.doDai(ten) > TEN_TOI_DA) {
			loi.add(new FieldError("tenToaNha", "Tên tòa nhà tối đa 100 ký tự"));
		}
		Integer soTang = NhapLieu.soTang(request.soTang(), loi);
		String diaChi = NhapLieu.chuanHoaChu(request.diaChiKhuVuc());
		if (diaChi.isEmpty()) {
			loi.add(new FieldError("diaChiKhuVuc", "Vui lòng nhập địa chỉ khu vực"));
		}
		else if (NhapLieu.doDai(diaChi) > DIA_CHI_TOI_DA) {
			loi.add(new FieldError("diaChiKhuVuc", "Địa chỉ khu vực tối đa 255 ký tự"));
		}
		if (!loi.isEmpty()) {
			throw CanHoLoi.duLieuKhongHopLe(loi);
		}
		return new ToaNhaMoi(ma, ten, soTang, diaChi);
	}

	/** Hai request vượt qua bước kiểm cùng lúc: đổi lỗi ràng buộc thành 409 của AC-3. */
	private static RuntimeException tuRangBuoc(DataIntegrityViolationException ex) {
		Optional<String> truong = LoiCoSoDuLieu.tenRangBuoc(ex).map(RANG_BUOC_TRUNG::get);
		if (truong.isEmpty()) {
			return ex;
		}
		return CanHoLoi.trung("maToaNha".equals(truong.get()), "tenToaNha".equals(truong.get()));
	}

	/** Dữ liệu tòa đã chuẩn hóa và hợp lệ. */
	record ToaNhaMoi(String ma, String ten, int soTang, String diaChi) {
	}

}
