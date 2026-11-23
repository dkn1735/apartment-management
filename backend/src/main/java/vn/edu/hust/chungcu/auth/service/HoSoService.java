package vn.edu.hust.chungcu.auth.service;

import java.util.EnumSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.dto.HoSoResponse;
import vn.edu.hust.chungcu.auth.dto.ChiTietVaiTro;
import vn.edu.hust.chungcu.auth.dto.PhamVi;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTroNguoiDung;
import vn.edu.hust.chungcu.auth.entity.NguoiDungHeThong;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.auth.repository.VaiTroQuery;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;

/**
 * Hồ sơ của người đang đăng nhập (spec 0004, mục Hồ sơ). Vai trò và trạng thái luôn đọc
 * từ DB, không lấy từ token, nên khóa tài khoản có hiệu lực ngay ở đây (AC-9).
 */
@Service
public class HoSoService {

	private final NguoiDungHeThongRepository nguoiDung;

	private final VaiTroQuery vaiTroQuery;

	private final PhamViTinhToan phamViTinhToan;

	private final AuthCookies cookies;

	public HoSoService(NguoiDungHeThongRepository nguoiDung, VaiTroQuery vaiTroQuery, PhamViTinhToan phamViTinhToan,
			AuthCookies cookies) {
		this.nguoiDung = nguoiDung;
		this.vaiTroQuery = vaiTroQuery;
		this.phamViTinhToan = phamViTinhToan;
		this.cookies = cookies;
	}

	public HoSoResponse cua(String maNguoiDung) {
		NguoiDungHeThong tk = this.nguoiDung.findById(maNguoiDung)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED,
					"Bạn cần đăng nhập để tiếp tục", List.of(), this.cookies.xoa()));
		if (!tk.dangHoatDong()) {
			throw new ApiException(HttpStatus.FORBIDDEN, AuthErrorCodes.AUTH_ACCOUNT_LOCKED,
					AuthErrorCodes.AUTH_ACCOUNT_LOCKED_MESSAGE, List.of(), this.cookies.xoa());
		}
		VaiTroNguoiDung vaiTro = this.vaiTroQuery.cua(maNguoiDung);
		return new HoSoResponse(tk.getMaNguoiDung(), tk.getHoTen(), tk.getSoDienThoai(), tk.getEmail(),
				tk.getTrangThaiTaiKhoan(), tk.isPhaiDoiMatKhau(), vaiTro.vaiTro(), phamVi(vaiTro), vaiTro.chiTiet());
	}

	// Cùng quy tắc với PhamViTruyCapService, chỉ khác nguồn vai trò (spec 0006, AC-6)
	private PhamVi phamVi(VaiTroNguoiDung vaiTro) {
		ChiTietVaiTro.VaiTroCuDan cuDan = vaiTro.chiTiet().cuDan();
		return this.phamViTinhToan.tinh(
				vaiTro.vaiTro().isEmpty() ? EnumSet.noneOf(VaiTro.class) : EnumSet.copyOf(vaiTro.vaiTro()),
				(cuDan != null) ? cuDan.maCuDan() : null);
	}

}
