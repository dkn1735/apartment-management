package vn.edu.hust.chungcu.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.dto.KiemTraDatLaiResponse;
import vn.edu.hust.chungcu.auth.dto.XacNhanDatLaiRequest;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository.YeuCauConHieuLuc;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponse;

/**
 * Kiểm và dùng link đặt lại mật khẩu (spec 0010, Luồng xác nhận). Mọi lý do token không
 * dùng được cho cùng một lỗi (AC-6). Quy tắc mật khẩu kiểm trước token; token hỏng thì
 * không chạy bcrypt (AC-9). Bcrypt chạy ngoài transaction; phần ghi ở
 * {@link GhiXacNhanQuenMatKhauService}.
 */
@Service
public class XacNhanQuenMatKhauService {

	private final QuenMatKhauRepository repository;

	private final PasswordEncoder encoder;

	private final GhiXacNhanQuenMatKhauService ghi;

	private final Clock clock;

	public XacNhanQuenMatKhauService(QuenMatKhauRepository repository, PasswordEncoder encoder,
			GhiXacNhanQuenMatKhauService ghi, Clock clock) {
		this.repository = repository;
		this.encoder = encoder;
		this.ghi = ghi;
		this.clock = clock;
	}

	public KiemTraDatLaiResponse kiemTra(String token) {
		return new KiemTraDatLaiResponse(conHieuLuc(token, now()).hetHanLuc());
	}

	/**
	 * @return số họ phiên đã thu hồi
	 */
	public int xacNhan(XacNhanDatLaiRequest request) {
		if (request.matKhauMoi().getBytes(StandardCharsets.UTF_8).length > MatKhauService.BCRYPT_TOI_DA_BYTE) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Dữ liệu không hợp lệ",
					List.of(new ErrorResponse.FieldError("matKhauMoi",
							"Mật khẩu mới quá dài, vui lòng dùng mật khẩu ngắn hơn.")));
		}
		YeuCauConHieuLuc yeuCau = conHieuLuc(request.token(), now());
		String hash = this.encoder.encode(request.matKhauMoi());
		// Lấy giờ lại sau bcrypt: hết hạn được kiểm lần nữa trong câu tiêu token
		return this.ghi.ghi(yeuCau.maNguoiDung(), RefreshTokenGenerator.sha256Hex(request.token()), hash, now());
	}

	private YeuCauConHieuLuc conHieuLuc(String token, Instant now) {
		if (!RefreshTokenGenerator.dungDinhDang(token)) {
			throw tokenKhongHopLe();
		}
		return this.repository.timConHieuLuc(RefreshTokenGenerator.sha256Hex(token), now)
			.orElseThrow(XacNhanQuenMatKhauService::tokenKhongHopLe);
	}

	private Instant now() {
		return Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
	}

	static ApiException tokenKhongHopLe() {
		return new ApiException(HttpStatus.BAD_REQUEST, AuthErrorCodes.AUTH_RESET_TOKEN_INVALID,
				AuthErrorCodes.AUTH_RESET_TOKEN_INVALID_MESSAGE);
	}

}
