package vn.edu.hust.chungcu.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.dto.DoiMatKhauRequest;
import vn.edu.hust.chungcu.auth.dto.DoiMatKhauResponse;
import vn.edu.hust.chungcu.auth.entity.NguoiDungHeThong;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.auth.repository.VaiTroQuery;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.common.error.ErrorResponse;

/**
 * Đổi mật khẩu của người đang đăng nhập (API-AUTH-04). Kiểm mật khẩu cũ và băm mật khẩu
 * mới (hai lần bcrypt) nằm ngoài transaction; phần ghi ở {@link GhiMatKhauMoiService}.
 * Phiên của máy đang đổi được giữ, các họ phiên khác bị thu hồi. Access token đã cấp cho
 * máy khác vẫn sống tới hết hạn (tối đa 15 phút), vì JWT không tra DB. Máy đang đổi nhận
 * access token mới có claim {@code phaiDoiMatKhau} đọc lại từ DB (spec 0008, AC-13).
 */
@Service
public class DoiMatKhauService {

	static final String MAT_KHAU_CU_SAI_MESSAGE = "Mật khẩu hiện tại không đúng.";

	private final NguoiDungHeThongRepository nguoiDung;

	private final MatKhauService matKhau;

	private final PasswordEncoder encoder;

	private final GhiMatKhauMoiService ghi;

	private final VaiTroQuery vaiTroQuery;

	private final AccessTokenService accessTokens;

	private final Clock clock;

	public DoiMatKhauService(NguoiDungHeThongRepository nguoiDung, MatKhauService matKhau, PasswordEncoder encoder,
			GhiMatKhauMoiService ghi, VaiTroQuery vaiTroQuery, AccessTokenService accessTokens, Clock clock) {
		this.nguoiDung = nguoiDung;
		this.matKhau = matKhau;
		this.encoder = encoder;
		this.ghi = ghi;
		this.vaiTroQuery = vaiTroQuery;
		this.accessTokens = accessTokens;
		this.clock = clock;
	}

	/**
	 * @param maHoPhien họ phiên trong access token của request ({@code sid}), được giữ
	 * lại
	 */
	public KetQuaDoiMatKhau doi(String maNguoiDung, UUID maHoPhien, DoiMatKhauRequest request) {
		if (request.matKhauMoi().getBytes(StandardCharsets.UTF_8).length > MatKhauService.BCRYPT_TOI_DA_BYTE) {
			throw loiO("matKhauMoi", "Mật khẩu mới quá dài, vui lòng dùng mật khẩu ngắn hơn.");
		}
		if (request.matKhauMoi().equals(request.matKhauCu())) {
			throw loiO("matKhauMoi", "Mật khẩu mới phải khác mật khẩu hiện tại");
		}
		NguoiDungHeThong tk = this.nguoiDung.findById(maNguoiDung)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED,
					"Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại."));
		if (!tk.dangHoatDong()) {
			throw DangNhapService.taiKhoanKhoa();
		}
		String hashCu = tk.getMatKhauHash();
		if (!this.matKhau.khop(request.matKhauCu(), hashCu)) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, AuthErrorCodes.AUTH_OLD_PASSWORD_INCORRECT,
					MAT_KHAU_CU_SAI_MESSAGE,
					List.of(new ErrorResponse.FieldError("matKhauCu", MAT_KHAU_CU_SAI_MESSAGE)));
		}
		String hashMoi = this.encoder.encode(request.matKhauMoi());
		// PostgreSQL lưu tới micro giây; cắt trước để giá trị trả về bằng giá trị đã lưu
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		this.ghi.ghi(maNguoiDung, hashCu, hashMoi, maHoPhien, now);
		// Token thiếu sid thì không có họ phiên để gắn token mới; người dùng làm mới như
		// thường
		String accessToken = null;
		if (maHoPhien != null) {
			NguoiDungHeThong sau = this.nguoiDung.findById(maNguoiDung).orElseThrow();
			accessToken = this.accessTokens
				.cap(maNguoiDung, maHoPhien, this.vaiTroQuery.cua(maNguoiDung).vaiTro(), sau.isPhaiDoiMatKhau(), now)
				.value();
		}
		return new KetQuaDoiMatKhau(new DoiMatKhauResponse(now), accessToken);
	}

	/**
	 * Body trả về và access token mới cho máy đang đổi; {@code accessToken} null khi
	 * token gửi lên không có {@code sid}.
	 */
	public record KetQuaDoiMatKhau(DoiMatKhauResponse data, String accessToken) {

		@Override
		public String toString() {
			return "KetQuaDoiMatKhau[data=" + this.data + "]";
		}

	}

	private static ApiException loiO(String field, String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_FAILED, "Dữ liệu không hợp lệ",
				List.of(new ErrorResponse.FieldError(field, message)));
	}

}
