package vn.edu.hust.chungcu.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.dto.DangNhapRequest;
import vn.edu.hust.chungcu.auth.dto.DangNhapResponse;
import vn.edu.hust.chungcu.auth.dto.DoiMatKhauRequest;
import vn.edu.hust.chungcu.auth.dto.DoiMatKhauResponse;
import vn.edu.hust.chungcu.auth.dto.HoSoResponse;
import vn.edu.hust.chungcu.auth.dto.LamMoiPhienResponse;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhepKhiPhaiDoiMatKhau;
import vn.edu.hust.chungcu.auth.phanquyen.CongKhai;
import vn.edu.hust.chungcu.auth.phanquyen.DaDangNhap;
import vn.edu.hust.chungcu.auth.service.DangNhapService;
import vn.edu.hust.chungcu.auth.service.DangXuatService;
import vn.edu.hust.chungcu.auth.service.DoiMatKhauService;
import vn.edu.hust.chungcu.auth.service.AuthCookies;
import vn.edu.hust.chungcu.auth.service.HoSoService;
import vn.edu.hust.chungcu.auth.service.LamMoiPhienService;
import vn.edu.hust.chungcu.auth.service.ThongTinThietBi;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.common.security.JwtConfig;

@Tag(name = "Xác thực")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final DangNhapService dangNhapService;

	private final HoSoService hoSoService;

	private final LamMoiPhienService lamMoiPhienService;

	private final DangXuatService dangXuatService;

	private final DoiMatKhauService doiMatKhauService;

	private final AuthCookies cookies;

	public AuthController(DangNhapService dangNhapService, HoSoService hoSoService,
			LamMoiPhienService lamMoiPhienService, DangXuatService dangXuatService, DoiMatKhauService doiMatKhauService,
			AuthCookies cookies) {
		this.doiMatKhauService = doiMatKhauService;
		this.dangNhapService = dangNhapService;
		this.hoSoService = hoSoService;
		this.lamMoiPhienService = lamMoiPhienService;
		this.dangXuatService = dangXuatService;
		this.cookies = cookies;
	}

	@Operation(summary = "Đăng nhập bằng số điện thoại hoặc email",
			description = "Đặt ba cookie HttpOnly: access_token, refresh_token, phien. Body không chứa token.")
	@CongKhai
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<DangNhapResponse>> dangNhap(@Valid @RequestBody DangNhapRequest request,
			HttpServletRequest http) {
		DangNhapService.KetQuaDangNhap ketQua = this.dangNhapService.dangNhap(request, thietBi(http));
		// Service đã commit; cookie chỉ được đặt sau khi phiên đã ghi xong
		ResponseEntity<ApiResponse<DangNhapResponse>> ok = ApiResponse.ok(ketQua.data(), "Đăng nhập thành công");
		return ResponseEntity.status(ok.getStatusCode())
			.headers(this.cookies.dat(ketQua.accessToken(), ketQua.refreshToken(), ketQua.refreshMaxAge()))
			.body(ok.getBody());
	}

	@Operation(summary = "Làm mới phiên bằng cookie refresh_token",
			description = "Đặt lại ba cookie ở mọi response 200. lamMoi true: vừa xoay ra refresh token mới. "
					+ "lamMoi false: token gửi lên vừa được xoay trong khoảng khoan dung, refresh_token là "
					+ "token đang sống được trả lại. Không có body.")
	@CongKhai
	@PostMapping("/refresh-token")
	public ResponseEntity<ApiResponse<LamMoiPhienResponse>> lamMoi(
			@CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshToken,
			HttpServletRequest http) {
		LamMoiPhienService.KetQuaLamMoi ketQua = this.lamMoiPhienService.lamMoi(refreshToken, thietBi(http));
		ResponseEntity<ApiResponse<LamMoiPhienResponse>> ok = ApiResponse.ok(ketQua.data(), "Làm mới phiên thành công");
		// Service đã commit; cookie chỉ được đặt sau khi xoay vòng đã ghi xong
		return ResponseEntity.status(ok.getStatusCode())
			.headers(this.cookies.dat(ketQua.accessToken(), ketQua.refreshToken(), ketQua.refreshMaxAge()))
			.body(ok.getBody());
	}

	@Operation(summary = "Đăng xuất phiên trên máy này",
			description = "Thu hồi họ phiên mà cookie refresh_token trỏ tới và xóa ba cookie. Luôn trả 200.")
	@CongKhai
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> dangXuat(
			@CookieValue(name = AuthCookies.REFRESH_TOKEN, required = false) String refreshToken) {
		this.dangXuatService.dangXuat(refreshToken);
		ResponseEntity<ApiResponse<Void>> ok = ApiResponse.ok(null, "Đăng xuất thành công");
		return ResponseEntity.status(ok.getStatusCode()).headers(this.cookies.xoa()).body(ok.getBody());
	}

	@Operation(summary = "Hồ sơ của người đang đăng nhập")
	@DaDangNhap
	@ChoPhepKhiPhaiDoiMatKhau
	@GetMapping("/profile")
	public ResponseEntity<ApiResponse<HoSoResponse>> hoSo(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok(this.hoSoService.cua(jwt.getSubject()), "Lấy thông tin tài khoản thành công");
	}

	@Operation(summary = "Đổi mật khẩu của người đang đăng nhập",
			description = "Kiểm mật khẩu hiện tại, lưu mật khẩu mới, giữ phiên trên máy này và thu hồi mọi phiên "
					+ "khác. Mật khẩu mới: 8 tới 128 ký tự, có cả chữ và số, khác mật khẩu hiện tại.")
	@DaDangNhap
	@ChoPhepKhiPhaiDoiMatKhau
	@PutMapping("/change-password")
	public ResponseEntity<ApiResponse<DoiMatKhauResponse>> doiMatKhau(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody DoiMatKhauRequest request) {
		DoiMatKhauService.KetQuaDoiMatKhau ketQua = this.doiMatKhauService.doi(jwt.getSubject(), hoPhien(jwt), request);
		ResponseEntity<ApiResponse<DoiMatKhauResponse>> ok = ApiResponse.ok(ketQua.data(), "Đổi mật khẩu thành công");
		if (ketQua.accessToken() == null) {
			return ok;
		}
		// Access token mới mang claim phaiDoiMatKhau = false, nên request kế tiếp chạy
		// ngay (spec 0008, AC-13)
		return ResponseEntity.status(ok.getStatusCode())
			.headers(this.cookies.datAccessToken(ketQua.accessToken()))
			.body(ok.getBody());
	}

	/** Claim {@code sid} của access token; thiếu hay sai dạng thì không giữ họ nào. */
	private static UUID hoPhien(Jwt jwt) {
		try {
			String sid = jwt.getClaimAsString(JwtConfig.CLAIM_SID);
			return (sid != null) ? UUID.fromString(sid) : null;
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}

	// getRemoteAddr đã qua forward-headers-strategy, nên là IP trình duyệt mà Next.js
	// chuyển tiếp
	private static ThongTinThietBi thietBi(HttpServletRequest http) {
		return new ThongTinThietBi(http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
	}

}
