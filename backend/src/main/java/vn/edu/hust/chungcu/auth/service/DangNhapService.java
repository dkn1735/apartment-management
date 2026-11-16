package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.dto.DangNhapRequest;
import vn.edu.hust.chungcu.auth.dto.DangNhapResponse;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.entity.NguoiDungHeThong;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.auth.repository.VaiTroQuery;
import vn.edu.hust.chungcu.common.config.AppProperties;
import vn.edu.hust.chungcu.common.error.ApiException;

/**
 * Luồng đăng nhập (spec 0004, mục Luồng đăng nhập). Kiểm mật khẩu ngoài transaction; chỉ
 * phần ghi DB nằm trong {@link PhienDangNhapService#taoPhien}.
 */
@Service
public class DangNhapService {

	private final NguoiDungHeThongRepository nguoiDung;

	private final PasswordEncoder passwordEncoder;

	private final PhienDangNhapService phienService;

	private final VaiTroQuery vaiTroQuery;

	private final AccessTokenService accessTokens;

	private final RefreshTokenGenerator refreshTokens;

	private final Clock clock;

	private final Duration refreshTtl;

	public DangNhapService(NguoiDungHeThongRepository nguoiDung, PasswordEncoder passwordEncoder,
			PhienDangNhapService phienService, VaiTroQuery vaiTroQuery, AccessTokenService accessTokens,
			RefreshTokenGenerator refreshTokens, Clock clock, AppProperties properties) {
		this.nguoiDung = nguoiDung;
		this.passwordEncoder = passwordEncoder;
		this.phienService = phienService;
		this.vaiTroQuery = vaiTroQuery;
		this.accessTokens = accessTokens;
		this.refreshTokens = refreshTokens;
		this.clock = clock;
		this.refreshTtl = properties.security().refreshTokenTtl();
	}

	public KetQuaDangNhap dangNhap(DangNhapRequest request) {
		NguoiDungHeThong tk = this.nguoiDung.findBySoDienThoai(request.taiKhoan())
			.filter(found -> this.passwordEncoder.matches(request.matKhau(), found.getMatKhauHash()))
			.orElseThrow(DangNhapService::saiThongTin);

		Instant now = Instant.now(this.clock);
		UUID maHoPhien = UUID.randomUUID();
		String refreshToken = this.refreshTokens.moi();
		this.phienService.taoPhien(new PhienDangNhapService.PhienMoi(tk.getMaNguoiDung(), maHoPhien,
				RefreshTokenGenerator.sha256Hex(refreshToken), now, now.plus(this.refreshTtl), null, null,
				tk.getMatKhauHash(), null));

		List<VaiTro> vaiTro = this.vaiTroQuery.cua(tk.getMaNguoiDung()).vaiTro();
		AccessTokenService.AccessToken accessToken = this.accessTokens.cap(tk.getMaNguoiDung(), maHoPhien, vaiTro, now);
		DangNhapResponse data = new DangNhapResponse(tk.getMaNguoiDung(), tk.getHoTen(), vaiTro,
				accessToken.hetHanLuc());
		return new KetQuaDangNhap(data, accessToken.value(), refreshToken);
	}

	private static ApiException saiThongTin() {
		return new ApiException(HttpStatus.UNAUTHORIZED, AuthErrorCodes.AUTH_INVALID_CREDENTIALS,
				AuthErrorCodes.AUTH_INVALID_CREDENTIALS_MESSAGE);
	}

	/** Kết quả cho controller: dữ liệu response và hai token để đặt cookie. */
	public record KetQuaDangNhap(DangNhapResponse data, String accessToken, String refreshToken) {

		@Override
		public String toString() {
			return "KetQuaDangNhap[maNguoiDung=" + this.data.maNguoiDung() + "]";
		}

	}

}
