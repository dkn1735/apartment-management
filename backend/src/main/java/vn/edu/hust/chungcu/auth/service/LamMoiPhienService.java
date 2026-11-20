package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
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
 * phiên, dưới khóa theo họ. Token mới là {@code keTiep} của token cũ, nên trong khoảng
 * khoan dung một token vừa xoay luôn được trả lại đúng token đang sống (AC-5, AC-17). Thu
 * hồi do dùng lại hay do khóa tài khoản vẫn commit dù phương thức ném lỗi
 * ({@code noRollbackFor}). Log không bao giờ chứa token hay hash (AC-16).
 */
@Service
public class LamMoiPhienService {

	private static final Logger log = LoggerFactory.getLogger(LamMoiPhienService.class);

	/**
	 * Luồng thường cần 1 bước, hai tab chạy đua cần tới 2; quá 3 lần xoay trong khoảng
	 * khoan dung là bất thường.
	 */
	static final int SO_BUOC_KE_TIEP_TOI_DA = 3;

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
		if (!RefreshTokenGenerator.dungDinhDang(refreshToken)) {
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
			return dungLaiTokenDaXoay(dongCu, refreshToken, thietBi, now);
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
		String tokenMoi = this.refreshTokens.keTiep(refreshToken);
		this.phien.save(new PhienDangNhap(dongCu.getMaNguoiDung(), maHoPhien, RefreshTokenGenerator.sha256Hex(tokenMoi),
				dongCu.getBatDauLuc(), now, hetHanMoi, thietBi.diaChiIp(), thietBi.userAgent()));

		log.info("Làm mới thành công: maNguoiDung={}", tk.getMaNguoiDung());
		return ketQua(true, tk, maHoPhien, tokenMoi, conLai(now, hetHanMoi), now);
	}

	/**
	 * Token đã bị xoay mà được dùng lại. Họ đã chết: chỉ từ chối (AC-3). Quá khoảng khoan
	 * dung: dấu hiệu bị đánh cắp, thu hồi cả họ (AC-4). Trong khoảng khoan dung (tab chạy
	 * đua, response bị mất): đi chuỗi {@code keTiep} tới dòng còn hiệu lực và trả lại
	 * đúng token đó, không ghi DB (AC-5, AC-17).
	 */
	private KetQuaLamMoi dungLaiTokenDaXoay(PhienDangNhap dongCu, String token, ThongTinThietBi thietBi, Instant now) {
		UUID maHoPhien = dongCu.getMaHoPhien();
		PhienDangNhap conSong = this.phien.timConHieuLuc(maHoPhien, now).orElseThrow(this::khongHopLe);
		boolean trongKhoangKhoanDung = !dongCu.getThuHoiLuc().plus(this.cauHinh.refreshReuseGrace()).isBefore(now);
		if (!trongKhoangKhoanDung) {
			this.phien.thuHoiHo(maHoPhien, now, LyDoThuHoi.DUNG_LAI_TOKEN_CU);
			log.warn(
					"Phát hiện dùng lại refresh token đã xoay vòng, đã thu hồi cả họ phiên: maNguoiDung={}, maHoPhien={}",
					dongCu.getMaNguoiDung(), maHoPhien);
			throw khongHopLe();
		}
		String tokenSong = timTheoChuoi(token, conSong.getTokenHash());
		if (tokenSong == null) {
			log.info("Làm mới thất bại: lyDo=KHONG_KHOP_CHUOI, maHoPhien={}", maHoPhien);
			throw khongHopLe();
		}
		NguoiDungHeThong tk = taiKhoan(dongCu);
		if (!tk.dangHoatDong()) {
			this.phien.thuHoiHo(maHoPhien, now, LyDoThuHoi.TAI_KHOAN_BI_KHOA);
			log.info("Làm mới thất bại: lyDo=TAI_KHOAN_KHOA, maNguoiDung={}", tk.getMaNguoiDung());
			throw taiKhoanKhoa();
		}
		boolean khacThietBi = !Objects.equals(conSong.getDiaChiIp(), thietBi.diaChiIp())
				|| !Objects.equals(conSong.getUserAgent(), thietBi.userAgent());
		log.info("Làm mới trả lại token đã cấp: maNguoiDung={}, khacThietBi={}", tk.getMaNguoiDung(), khacThietBi);
		return ketQua(false, tk, maHoPhien, tokenSong, conLai(now, conSong.getHetHanLuc()), now);
	}

	/**
	 * Đi chuỗi {@code keTiep} từ {@code token}, tối đa {@link #SO_BUOC_KE_TIEP_TOI_DA}
	 * bước, tới token có SHA-256 bằng {@code hashCanTim}; không khớp thì {@code null}.
	 */
	private String timTheoChuoi(String token, String hashCanTim) {
		String s = token;
		for (int buoc = 0; buoc < SO_BUOC_KE_TIEP_TOI_DA; buoc++) {
			s = this.refreshTokens.keTiep(s);
			if (RefreshTokenGenerator.sha256Hex(s).equals(hashCanTim)) {
				return s;
			}
		}
		return null;
	}

	private KetQuaLamMoi ketQua(boolean lamMoi, NguoiDungHeThong tk, UUID maHoPhien, String refreshToken,
			Duration refreshMaxAge, Instant now) {
		List<VaiTro> vaiTro = this.vaiTroQuery.cua(tk.getMaNguoiDung()).vaiTro();
		AccessTokenService.AccessToken accessToken = this.accessTokens.cap(tk.getMaNguoiDung(), maHoPhien, vaiTro, now);
		LamMoiPhienResponse data = new LamMoiPhienResponse(lamMoi, tk.getMaNguoiDung(), tk.getHoTen(), vaiTro,
				accessToken.hetHanLuc());
		return new KetQuaLamMoi(data, accessToken.value(), refreshToken, refreshMaxAge);
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
	 * Kết quả cho controller. Mọi kết quả đều có đủ token để đặt ba cookie, kể cả khi
	 * {@code data.lamMoi()} là {@code false} (token đang sống được trả lại).
	 */
	public record KetQuaLamMoi(LamMoiPhienResponse data, String accessToken, String refreshToken,
			Duration refreshMaxAge) {

		@Override
		public String toString() {
			return "KetQuaLamMoi[maNguoiDung=" + this.data.maNguoiDung() + ", lamMoi=" + this.data.lamMoi() + "]";
		}

	}

}
