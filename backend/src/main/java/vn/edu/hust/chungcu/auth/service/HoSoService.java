package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.dto.HoSoResponse;
import vn.edu.hust.chungcu.auth.dto.PhamVi;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTroNguoiDung;
import vn.edu.hust.chungcu.auth.entity.NguoiDungHeThong;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.auth.repository.VaiTroQuery;
import vn.edu.hust.chungcu.canho.service.ToaNhaService;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.cutru.service.CuTruService;

/**
 * Hồ sơ của người đang đăng nhập (spec 0004, mục Hồ sơ). Vai trò và trạng thái luôn đọc
 * từ DB, không lấy từ token, nên khóa tài khoản có hiệu lực ngay ở đây (AC-9).
 */
@Service
public class HoSoService {

	private final NguoiDungHeThongRepository nguoiDung;

	private final VaiTroQuery vaiTroQuery;

	private final ToaNhaService toaNhaService;

	private final CuTruService cuTruService;

	private final AuthCookies cookies;

	private final Clock clock;

	public HoSoService(NguoiDungHeThongRepository nguoiDung, VaiTroQuery vaiTroQuery, ToaNhaService toaNhaService,
			CuTruService cuTruService, AuthCookies cookies, Clock clock) {
		this.nguoiDung = nguoiDung;
		this.vaiTroQuery = vaiTroQuery;
		this.toaNhaService = toaNhaService;
		this.cuTruService = cuTruService;
		this.cookies = cookies;
		this.clock = clock;
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
				tk.getTrangThaiTaiKhoan(), vaiTro.vaiTro(), phamVi(vaiTro), vaiTro.chiTiet());
	}

	private PhamVi phamVi(VaiTroNguoiDung vaiTro) {
		List<String> maToaNha = vaiTro.coVaiTroKhacCuDan() ? this.toaNhaService.danhSachMaToaNha() : List.of();
		List<String> maCanHo = vaiTro.co(VaiTro.CU_DAN)
				// Clock mang múi giờ app.timezone, nên đây là ngày theo giờ Việt Nam
				? this.cuTruService.maCanHoDangCuTru(vaiTro.chiTiet().cuDan().maCuDan(), LocalDate.now(this.clock))
				: List.of();
		return new PhamVi(maToaNha, maCanHo);
	}

}
