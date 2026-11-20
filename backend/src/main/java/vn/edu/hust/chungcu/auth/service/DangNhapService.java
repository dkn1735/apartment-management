package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
 * Luồng đăng nhập (spec 0004, mục Luồng đăng nhập). Bước 1 tới 6 (gồm bcrypt) nằm ngoài
 * transaction; chỉ phần ghi DB nằm trong {@link PhienDangNhapService#taoPhien}. Log không
 * bao giờ chứa mật khẩu, token, hash hay giá trị {@code taiKhoan} (AC-16).
 */
@Service
public class DangNhapService {

	private static final Logger log = LoggerFactory.getLogger(DangNhapService.class);

	private final NguoiDungHeThongRepository nguoiDung;

	private final MatKhauService matKhau;

	private final PhienDangNhapService phienService;

	private final VaiTroQuery vaiTroQuery;

	private final AccessTokenService accessTokens;

	private final RefreshTokenGenerator refreshTokens;

	private final Clock clock;

	private final Duration refreshTtl;

	public DangNhapService(NguoiDungHeThongRepository nguoiDung, MatKhauService matKhau,
			PhienDangNhapService phienService, VaiTroQuery vaiTroQuery, AccessTokenService accessTokens,
			RefreshTokenGenerator refreshTokens, Clock clock, AppProperties properties) {
		this.nguoiDung = nguoiDung;
		this.matKhau = matKhau;
		this.phienService = phienService;
		this.vaiTroQuery = vaiTroQuery;
		this.accessTokens = accessTokens;
		this.refreshTokens = refreshTokens;
		this.clock = clock;
		this.refreshTtl = properties.security().refreshTokenTtl();
	}

	public KetQuaDangNhap dangNhap(DangNhapRequest request, ThongTinThietBi thietBi) {
		Optional<NguoiDungHeThong> timThay = TaiKhoanDangNhap.parse(request.taiKhoan()).flatMap(this::tim);
		if (timThay.isEmpty()) {
			this.matKhau.soKhopGia(request.matKhau());
			log.info("Đăng nhập thất bại: lyDo=SAI_THONG_TIN");
			throw saiThongTin();
		}
		NguoiDungHeThong tk = timThay.get();
		if (!this.matKhau.khop(request.matKhau(), tk.getMatKhauHash())) {
			log.info("Đăng nhập thất bại: lyDo=SAI_THONG_TIN, maNguoiDung={}", tk.getMaNguoiDung());
			throw saiThongTin();
		}
		if (!tk.dangHoatDong()) {
			log.info("Đăng nhập thất bại: lyDo=TAI_KHOAN_KHOA, maNguoiDung={}", tk.getMaNguoiDung());
			throw taiKhoanKhoa();
		}
		// Băm lại (khoảng 250 ms) trước khi mở transaction, để không giữ kết nối DB
		String hashMoi = this.matKhau.hashMoiNeuCan(request.matKhau(), tk.getMatKhauHash()).orElse(null);

		// PostgreSQL lưu tới micro giây; cắt trước để giá trị đã lưu bằng giá trị đã so
		// (spec
		// 0005)
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		UUID maHoPhien = UUID.randomUUID();
		String refreshToken = this.refreshTokens.moi();
		this.phienService.taoPhien(new PhienDangNhapService.PhienMoi(tk.getMaNguoiDung(), maHoPhien,
				RefreshTokenGenerator.sha256Hex(refreshToken), now, now.plus(this.refreshTtl), thietBi.diaChiIp(),
				thietBi.userAgent(), tk.getMatKhauHash(), hashMoi));

		List<VaiTro> vaiTro = this.vaiTroQuery.cua(tk.getMaNguoiDung()).vaiTro();
		AccessTokenService.AccessToken accessToken = this.accessTokens.cap(tk.getMaNguoiDung(), maHoPhien, vaiTro, now);
		log.info("Đăng nhập thành công: maNguoiDung={}", tk.getMaNguoiDung());
		DangNhapResponse data = new DangNhapResponse(tk.getMaNguoiDung(), tk.getHoTen(), vaiTro,
				accessToken.hetHanLuc());
		return new KetQuaDangNhap(data, accessToken.value(), refreshToken, this.refreshTtl);
	}

	private Optional<NguoiDungHeThong> tim(TaiKhoanDangNhap taiKhoan) {
		return switch (taiKhoan) {
			case TaiKhoanDangNhap.Email email -> this.nguoiDung.findByEmail(email.value());
			case TaiKhoanDangNhap.SoDienThoai so -> this.nguoiDung.findBySoDienThoai(so.value());
		};
	}

	/** Sai tài khoản và sai mật khẩu dùng cùng mã và cùng câu (AC-3). */
	private static ApiException saiThongTin() {
		return new ApiException(HttpStatus.UNAUTHORIZED, AuthErrorCodes.AUTH_INVALID_CREDENTIALS,
				AuthErrorCodes.AUTH_INVALID_CREDENTIALS_MESSAGE);
	}

	static ApiException taiKhoanKhoa() {
		return new ApiException(HttpStatus.FORBIDDEN, AuthErrorCodes.AUTH_ACCOUNT_LOCKED,
				AuthErrorCodes.AUTH_ACCOUNT_LOCKED_MESSAGE);
	}

	/**
	 * Kết quả cho controller: dữ liệu response, hai token và {@code Max-Age} của cookie
	 * refresh để đặt cookie.
	 */
	public record KetQuaDangNhap(DangNhapResponse data, String accessToken, String refreshToken,
			Duration refreshMaxAge) {

		@Override
		public String toString() {
			return "KetQuaDangNhap[maNguoiDung=" + this.data.maNguoiDung() + "]";
		}

	}

}
