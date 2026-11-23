package vn.edu.hust.chungcu.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.dto.KiemTraDatLaiRequest;
import vn.edu.hust.chungcu.auth.dto.KiemTraDatLaiResponse;
import vn.edu.hust.chungcu.auth.dto.XacNhanDatLaiRequest;
import vn.edu.hust.chungcu.auth.dto.YeuCauDatLaiRequest;
import vn.edu.hust.chungcu.auth.phanquyen.CongKhai;
import vn.edu.hust.chungcu.auth.service.AuthCookies;
import vn.edu.hust.chungcu.auth.service.QuenMatKhauService;
import vn.edu.hust.chungcu.auth.service.XacNhanQuenMatKhauService;
import vn.edu.hust.chungcu.common.api.ApiResponse;

/**
 * Quên mật khẩu qua email (spec 0010). Ba đường công khai; token chỉ đi trong body, không
 * bao giờ trong URL (AC-15). Tách khỏi {@link AuthController} để mỗi lớp giữ một việc.
 */
@Tag(name = "Xác thực")
@RestController
@RequestMapping("/api/v1/auth/password-reset")
public class QuenMatKhauController {

	private final QuenMatKhauService quenMatKhau;

	private final XacNhanQuenMatKhauService xacNhan;

	private final AuthCookies cookies;

	public QuenMatKhauController(QuenMatKhauService quenMatKhau, XacNhanQuenMatKhauService xacNhan,
			AuthCookies cookies) {
		this.quenMatKhau = quenMatKhau;
		this.xacNhan = xacNhan;
		this.cookies = cookies;
	}

	@Operation(operationId = "yeuCauDatLaiMatKhauEmail", summary = "Yêu cầu link đặt lại mật khẩu qua email",
			description = "Luôn trả cùng một câu dù email có thuộc tài khoản nào hay không. Email gửi sau khi trả "
					+ "phản hồi; link sống 30 phút và chỉ dùng được một lần.")
	@CongKhai
	@PostMapping("/request")
	public ResponseEntity<ApiResponse<Void>> yeuCau(@Valid @RequestBody YeuCauDatLaiRequest request) {
		this.quenMatKhau.yeuCau(request);
		return ApiResponse.ok(null, QuenMatKhauService.PHAN_HOI_CHUNG);
	}

	@Operation(operationId = "kiemTraTokenDatLai", summary = "Kiểm link đặt lại mật khẩu còn dùng được không",
			description = "Không đổi dữ liệu. Mọi lý do token không dùng được trả cùng mã AUTH_RESET_TOKEN_INVALID.")
	@CongKhai
	@PostMapping("/check")
	public ResponseEntity<ApiResponse<KiemTraDatLaiResponse>> kiemTra(
			@Valid @RequestBody KiemTraDatLaiRequest request) {
		return ApiResponse.ok(this.xacNhan.kiemTra(request.token()), "Link đặt lại mật khẩu hợp lệ");
	}

	@Operation(operationId = "xacNhanDatLaiMatKhauEmail", summary = "Đặt mật khẩu mới bằng link trong email",
			description = "Dùng token một lần, đổi mật khẩu, thu hồi mọi phiên đăng nhập kể cả phiên của máy này "
					+ "và xóa ba cookie xác thực. Mật khẩu mới: 8 tới 128 ký tự, có cả chữ và số.")
	@CongKhai
	@PostMapping("/confirm")
	public ResponseEntity<ApiResponse<Void>> xacNhan(@Valid @RequestBody XacNhanDatLaiRequest request) {
		this.xacNhan.xacNhan(request);
		ResponseEntity<ApiResponse<Void>> ok = ApiResponse.ok(null, "Đặt lại mật khẩu thành công");
		// Service đã commit; mọi phiên của tài khoản đã bị thu hồi, nên cookie ở máy này
		// cũng không còn dùng được
		return ResponseEntity.status(ok.getStatusCode()).headers(this.cookies.xoa()).body(ok.getBody());
	}

}
