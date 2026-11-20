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
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.LamMoiPhienResponse;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.entity.NguoiDungHeThong;
import vn.edu.hust.chungcu.auth.entity.PhienDangNhap;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.auth.repository.PhienDangNhapRepository;
import vn.edu.hust.chungcu.auth.repository.VaiTroQuery;
import vn.edu.hust.chungcu.common.config.AppProperties;
import vn.edu.hust.chungcu.common.error.ApiException;

/**
 * Luồng làm mới (spec 0005, mục Luồng làm mới): xoay vòng refresh token trong cùng họ
 * phiên, dưới khóa theo họ. Thu hồi do dùng lại hay do khóa tài khoản vẫn commit dù
 * phương thức ném lỗi ({@code noRollbackFor}). Log không bao giờ chứa token hay hash
 * (AC-16).
 */
@Service
public class LamMoiPhienService {

	private static final Logger log = LoggerFactory.getLogger(LamMoiPhienService.class);

	private final PhienDangNhapRepository phien;

	private final NguoiDungHeThongRepository nguoiDung;

	private final VaiTroQuery vaiTroQuery;

	private final AccessTokenService accessTokens;

	private final RefreshTokenGenerator refreshTokens;

	private final AuthCookies cookies;

	private final Clock clock;

	private final AppProperties.Security cauHinh;

	public LamMoiPhienService(PhienDangNhapRepository phien, NguoiDungHeThongRepository nguoiDung,
			VaiTroQuery vaiTroQuery, AccessTokenService accessTokens, RefreshTokenGenerator refreshTokens,
			AuthCookies cookies, Clock clock, AppProperties properties) {
		this.phien = phien;
		this.nguoiDung = nguoiDung;
		this.vaiTroQuery = vaiTroQuery;
		this.accessTokens = accessTokens;
		this.refreshTokens = refreshTokens;
		this.cookies = cookies;
		this.clock = clock;
		this.cauHinh = properties.security();
	}

	@Transactional(noRollbackFor = ApiException.class)
	public KetQuaLamMoi lamMoi(String refreshToken, ThongTinThietBi thietBi) {
		if (refreshToken == null || refreshToken.isBlank()) {
			throw khongHopLe();
		}
		String hash = RefreshTokenGenerator.sha256Hex(refreshToken);
		UUID maHoPhien = this.phien.timHoPhien(hash).orElseThrow(this::khongHopLe);
		this.phien.khoaHo(maHoPhien);
		// Đọc lại dưới khóa: trạng thái mới nhất sau mọi request ghi chạy trước trên họ
		// này
		PhienDangNhap dongCu = this.phien.findByTokenHash(hash).orElseThrow(this::khongHopLe);
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);

		if (LyDoThuHoi.DA_XOAY_VONG.equals(dongCu.getLyDoThuHoi())) {
			return dungLaiTokenDaXoay(dongCu, now);
		}
		if (!dongCu.conHieuLuc(now)) {
			throw khongHopLe();
		}
		NguoiDungHeThong tk = taiKhoan(dongCu);
		if (!tk.dangHoatDong()) {
			this.phien.thuHoiHo(maHoPhien, now, LyDoThuHoi.TAI_KHOAN_BI_KHOA);
			log.info("Làm mới thất bại: lyDo=TAI_KHOAN_KHOA, maNguoiDung={}", tk.getMaNguoiDung());
			throw taiKhoanKhoa();
		}

		Instant hetHanMoi = min(now.plus(this.cauHinh.refreshTokenTtl()),
				dongCu.getBatDauLuc().plus(this.cauHinh.sessionMaxLifetime()));
		if (!hetHanMoi.isAfter(now)) {
			throw khongHopLe();
		}
		dongCu.thuHoi(now, LyDoThuHoi.DA_XOAY_VONG);
		// Ghi việc thu hồi trước, để index duy nhất một phần không thấy hai dòng chưa thu
		// hồi
		this.phien.saveAndFlush(dongCu);
		String tokenMoi = this.refreshTokens.moi();
		this.phien.save(new PhienDangNhap(dongCu.getMaNguoiDung(), maHoPhien, RefreshTokenGenerator.sha256Hex(tokenMoi),
				dongCu.getBatDauLuc(), now, hetHanMoi, thietBi.diaChiIp(), thietBi.userAgent()));

		List<VaiTro> vaiTro = this.vaiTroQuery.cua(tk.getMaNguoiDung()).vaiTro();
		AccessTokenService.AccessToken accessToken = this.accessTokens.cap(tk.getMaNguoiDung(), maHoPhien, vaiTro, now);
		log.info("Làm mới thành công: maNguoiDung={}", tk.getMaNguoiDung());
		LamMoiPhienResponse data = new LamMoiPhienResponse(true, tk.getMaNguoiDung(), tk.getHoTen(), vaiTro,
				accessToken.hetHanLuc());
		return new KetQuaLamMoi(data, accessToken.value(), tokenMoi, conLai(now, hetHanMoi));
	}

	/**
	 * Token đã bị xoay mà được dùng lại: trong khoảng khoan dung là tab chạy đua (AC-5),
	 * quá khoảng đó là dấu hiệu bị đánh cắp và cả họ bị thu hồi (AC-4). Họ đã chết thì
	 * chỉ từ chối (AC-3).
	 */
	private KetQuaLamMoi dungLaiTokenDaXoay(PhienDangNhap dongCu, Instant now) {
		UUID maHoPhien = dongCu.getMaHoPhien();
		if (!this.phien.hoConHieuLuc(maHoPhien, now)) {
			throw khongHopLe();
		}
		boolean trongKhoangKhoanDung = !dongCu.getThuHoiLuc().plus(this.cauHinh.refreshReuseGrace()).isBefore(now);
		if (!trongKhoangKhoanDung) {
			this.phien.thuHoiHo(maHoPhien, now, LyDoThuHoi.DUNG_LAI_TOKEN_CU);
			log.warn(
					"Phát hiện dùng lại refresh token đã xoay vòng, đã thu hồi cả họ phiên: maNguoiDung={}, maHoPhien={}",
					dongCu.getMaNguoiDung(), maHoPhien);
			throw khongHopLe();
		}
		NguoiDungHeThong tk = taiKhoan(dongCu);
		if (!tk.dangHoatDong()) {
			throw khongHopLe();
		}
		List<VaiTro> vaiTro = this.vaiTroQuery.cua(tk.getMaNguoiDung()).vaiTro();
		return new KetQuaLamMoi(new LamMoiPhienResponse(false, tk.getMaNguoiDung(), tk.getHoTen(), vaiTro, null), null,
				null, null);
	}

	private NguoiDungHeThong taiKhoan(PhienDangNhap dong) {
		Optional<NguoiDungHeThong> tk = this.nguoiDung.findById(dong.getMaNguoiDung());
		return tk.orElseThrow(this::khongHopLe);
	}

	/** Số giây còn lại tới {@code hetHan}, làm tròn lên và ít nhất 1 (AC-2). */
	static Duration conLai(Instant now, Instant hetHan) {
		Duration d = Duration.between(now, hetHan);
		long giay = d.getSeconds() + ((d.getNano() > 0) ? 1 : 0);
		return Duration.ofSeconds(Math.max(1, giay));
	}

	private static Instant min(Instant a, Instant b) {
		return a.isBefore(b) ? a : b;
	}

	private ApiException khongHopLe() {
		return new ApiException(HttpStatus.UNAUTHORIZED, AuthErrorCodes.AUTH_REFRESH_INVALID,
				AuthErrorCodes.AUTH_REFRESH_INVALID_MESSAGE, List.of(), this.cookies.xoa());
	}

	private ApiException taiKhoanKhoa() {
		return new ApiException(HttpStatus.FORBIDDEN, AuthErrorCodes.AUTH_ACCOUNT_LOCKED,
				AuthErrorCodes.AUTH_ACCOUNT_LOCKED_MESSAGE, List.of(), this.cookies.xoa());
	}

	/**
	 * Kết quả cho controller. Khi {@code data.lamMoi()} là {@code false} thì không có
	 * token và không đặt cookie.
	 */
	public record KetQuaLamMoi(LamMoiPhienResponse data, String accessToken, String refreshToken,
			Duration refreshMaxAge) {

		@Override
		public String toString() {
			return "KetQuaLamMoi[maNguoiDung=" + this.data.maNguoiDung() + ", lamMoi=" + this.data.lamMoi() + "]";
		}

	}

}
