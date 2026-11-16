package vn.edu.hust.chungcu.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.dto.DangNhapRequest;
import vn.edu.hust.chungcu.auth.dto.DangNhapResponse;
import vn.edu.hust.chungcu.auth.dto.HoSoResponse;
import vn.edu.hust.chungcu.auth.service.DangNhapService;
import vn.edu.hust.chungcu.auth.service.AuthCookies;
import vn.edu.hust.chungcu.auth.service.HoSoService;
import vn.edu.hust.chungcu.auth.service.ThongTinThietBi;
import vn.edu.hust.chungcu.common.api.ApiResponse;

@Tag(name = "Xác thực")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final DangNhapService dangNhapService;

	private final HoSoService hoSoService;

	private final AuthCookies cookies;

	public AuthController(DangNhapService dangNhapService, HoSoService hoSoService, AuthCookies cookies) {
		this.dangNhapService = dangNhapService;
		this.hoSoService = hoSoService;
		this.cookies = cookies;
	}

	@Operation(summary = "Đăng nhập bằng số điện thoại hoặc email",
			description = "Đặt ba cookie HttpOnly: access_token, refresh_token, phien. Body không chứa token.")
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<DangNhapResponse>> dangNhap(@Valid @RequestBody DangNhapRequest request,
			HttpServletRequest http) {
		// getRemoteAddr đã qua forward-headers-strategy, nên là IP trình duyệt mà Next.js
		// chuyển tiếp
		ThongTinThietBi thietBi = new ThongTinThietBi(http.getRemoteAddr(), http.getHeader(HttpHeaders.USER_AGENT));
		DangNhapService.KetQuaDangNhap ketQua = this.dangNhapService.dangNhap(request, thietBi);
		// Service đã commit; cookie chỉ được đặt sau khi phiên đã ghi xong
		ResponseEntity<ApiResponse<DangNhapResponse>> ok = ApiResponse.ok(ketQua.data(), "Đăng nhập thành công");
		return ResponseEntity.status(ok.getStatusCode())
			.headers(this.cookies.dat(ketQua.accessToken(), ketQua.refreshToken()))
			.body(ok.getBody());
	}

	@Operation(summary = "Hồ sơ của người đang đăng nhập")
	@GetMapping("/profile")
	public ResponseEntity<ApiResponse<HoSoResponse>> hoSo(@AuthenticationPrincipal Jwt jwt) {
		return ApiResponse.ok(this.hoSoService.cua(jwt.getSubject()), "Lấy thông tin tài khoản thành công");
	}

}
