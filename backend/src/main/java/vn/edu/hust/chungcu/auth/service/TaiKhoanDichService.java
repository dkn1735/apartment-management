package vn.edu.hust.chungcu.auth.service;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTroTaiKhoan;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanQuery;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanQuery.DongTaiKhoan;
import vn.edu.hust.chungcu.common.error.ApiException;

/**
 * Khóa dòng tài khoản đích rồi mới kiểm phạm vi (spec 0008, AC-15), cho mọi thao tác ghi.
 * Giữ khóa tới hết transaction của nơi gọi, nên Admin không đổi được vai trò của tài
 * khoản giữa lúc BQL kiểm phạm vi và lúc BQL ghi.
 */
@Service
public class TaiKhoanDichService {

	private static final Pattern MA_NGUOI_DUNG = Pattern.compile("^ND[0-9]{8}$");

	private final TaiKhoanQuery query;

	private final TaiKhoanPhamViService phamVi;

	public TaiKhoanDichService(TaiKhoanQuery query, TaiKhoanPhamViService phamVi) {
		this.query = query;
		this.phamVi = phamVi;
	}

	/** Tài khoản đích đã khóa, cùng vai trò của nó theo thứ tự enum. */
	public record TaiKhoanDich(DongTaiKhoan dong, List<VaiTroTaiKhoan> vaiTro) {

		public List<VaiTro> maVaiTro() {
			return this.vaiTro.stream().map(VaiTroTaiKhoan::vaiTro).toList();
		}

		public boolean co(VaiTro v) {
			return this.vaiTro.stream().anyMatch((x) -> x.vaiTro() == v);
		}

		public boolean dangHoatDong() {
			return TrangThaiTaiKhoan.HOAT_DONG.equals(this.dong.trangThaiTaiKhoan());
		}

	}

	/** Sai dạng, không tồn tại, hay ngoài phạm vi đều 404 cùng câu (AC-6). */
	@Transactional(propagation = Propagation.MANDATORY)
	public TaiKhoanDich khoa(NguoiQuanLy nguoi, String maNguoiDung) {
		if (maNguoiDung == null || !MA_NGUOI_DUNG.matcher(maNguoiDung).matches()) {
			throw ApiException.notFound(TaiKhoanPhamViService.KHONG_TIM_THAY);
		}
		DongTaiKhoan dong = this.query.khoa(maNguoiDung)
			.orElseThrow(() -> ApiException.notFound(TaiKhoanPhamViService.KHONG_TIM_THAY));
		List<VaiTroTaiKhoan> vaiTro = this.query.vaiTroCua(List.of(maNguoiDung)).get(maNguoiDung);
		TaiKhoanDich dich = new TaiKhoanDich(dong, vaiTro);
		this.phamVi.kiemThay(nguoi, dich.maVaiTro());
		return dich;
	}

}
