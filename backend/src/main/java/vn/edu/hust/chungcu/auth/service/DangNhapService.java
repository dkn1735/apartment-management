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
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Luồng đăng nhập (spec 0004, mục Luồng đăng nhập; spec 0009 thêm khóa tạm). Giữ lượt và
 * bcrypt nằm ngoài transaction; chỉ phần ghi phiên nằm trong
 * {@link PhienDangNhapService#taoPhien}. Log không bao giờ chứa mật khẩu, token, hash hay
 * giá trị {@code taiKhoan} (spec 0004 AC-16, spec 0009 AC-17).
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

	private final NhatKyService nhatKy;

	private final KhoaTamDangNhapService khoaTam;

	public DangNhapService(NguoiDungHeThongRepository nguoiDung, MatKhauService matKhau,
			PhienDangNhapService phienService, VaiTroQuery vaiTroQuery, AccessTokenService accessTokens,
			RefreshTokenGenerator refreshTokens, Clock clock, AppProperties properties, NhatKyService nhatKy,
			KhoaTamDangNhapService khoaTam) {
		this.nhatKy = nhatKy;
		this.khoaTam = khoaTam;
		this.nguoiDung = nguoiDung;
		this.matKhau = matKhau;
		this.phienService = phienService;
		this.vaiTroQuery = vaiTroQuery;
		this.accessTokens = accessTokens;
		this.refreshTokens = refreshTokens;
		this.clock = clock;
		this.refreshTtl = properties.security().refreshTokenTtl();
	}

	/**
	 * Thứ tự cố định của spec 0009 (mục Luồng đăng nhập mới): giữ lượt trước bcrypt, nên
	 * đang khóa thì không so mật khẩu và không ghi gì; định danh ảo đi cùng đường với tài
	 * khoản thật.
	 */
	public KetQuaDangNhap dangNhap(DangNhapRequest request, ThongTinThietBi thietBi) {
		// PostgreSQL lưu tới micro giây; cắt trước để giá trị đã lưu bằng giá trị đã so
		// (spec 0005). Lấy một lần, dùng cho mọi bước (spec 0009)
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		Optional<TaiKhoanDangNhap> dinhDanh = TaiKhoanDangNhap.parse(request.taiKhoan());
		if (dinhDanh.isEmpty()) {
			// Sai định dạng: không đếm (AC-5)
			this.matKhau.soKhopGia(request.matKhau());
			log.info("Đăng nhập thất bại: lyDo=SAI_THONG_TIN");
			ghiThatBai(null, ChiTietDangNhapThatBai.SAI_THONG_TIN);
			throw saiThongTin();
		}
		Optional<NguoiDungHeThong> timThay = tim(dinhDanh.get());
		Optional<String> ma = timThay.map(NguoiDungHeThong::getMaNguoiDung);
		LuotThu luot = this.khoaTam.giuLuot(ma, dinhDanh.get(), now);
		if (luot instanceof LuotThu.DangKhoa dangKhoa) {
			log.info("Đăng nhập thất bại: lyDo=KHOA_TAM{}", ma.map((m) -> ", maNguoiDung=" + m).orElse(""));
			throw KhoaTamDangNhap.quaNhieuLan(now, dangKhoa.khoaDen());
		}
		if (timThay.isEmpty()) {
			this.matKhau.soKhopGia(request.matKhau());
			log.info("Đăng nhập thất bại: lyDo=SAI_THONG_TIN");
			ghiThatBai(null, ChiTietDangNhapThatBai.SAI_THONG_TIN);
			throw (luot instanceof LuotThu.LuotCuoi cuoi) ? KhoaTamDangNhap.quaNhieuLan(now, cuoi.khoaDen())
					: saiThongTin();
		}
		NguoiDungHeThong tk = timThay.get();
		if (!this.matKhau.khop(request.matKhau(), tk.getMatKhauHash())) {
			log.info("Đăng nhập thất bại: lyDo=SAI_THONG_TIN, maNguoiDung={}", tk.getMaNguoiDung());
			ghiThatBai(tk.getMaNguoiDung(), ChiTietDangNhapThatBai.SAI_THONG_TIN);
			if (luot instanceof LuotThu.LuotCuoi cuoi) {
				ghiKhoaTam(tk.getMaNguoiDung(), cuoi.khoaDen());
				throw KhoaTamDangNhap.quaNhieuLan(now, cuoi.khoaDen());
			}
			throw saiThongTin();
		}
		if (!tk.dangHoatDong()) {
			// Mật khẩu khớp: đặt lại bộ đếm cả ở nhánh 403 (AC-4)
			this.khoaTam.datLai(tk.getMaNguoiDung());
			log.info("Đăng nhập thất bại: lyDo=TAI_KHOAN_KHOA, maNguoiDung={}", tk.getMaNguoiDung());
			ghiThatBai(tk.getMaNguoiDung(), ChiTietDangNhapThatBai.TAI_KHOAN_KHOA);
			throw taiKhoanKhoa();
		}
		// Băm lại (khoảng 250 ms) trước khi mở transaction, để không giữ kết nối DB
		String hashMoi = this.matKhau.hashMoiNeuCan(request.matKhau(), tk.getMatKhauHash()).orElse(null);

		// taoPhien cũng đặt lại bộ đếm, trong cùng transaction (AC-4)
		UUID maHoPhien = UUID.randomUUID();
		String refreshToken = this.refreshTokens.moi();
		this.phienService.taoPhien(new PhienDangNhapService.PhienMoi(tk.getMaNguoiDung(), maHoPhien,
				RefreshTokenGenerator.sha256Hex(refreshToken), now, now.plus(this.refreshTtl), thietBi.diaChiIp(),
				thietBi.userAgent(), tk.getMatKhauHash(), hashMoi));

		List<VaiTro> vaiTro = this.vaiTroQuery.cua(tk.getMaNguoiDung()).vaiTro();
		AccessTokenService.AccessToken accessToken = this.accessTokens.cap(tk.getMaNguoiDung(), maHoPhien, vaiTro,
				tk.isPhaiDoiMatKhau(), now);
		log.info("Đăng nhập thành công: maNguoiDung={}", tk.getMaNguoiDung());
		DangNhapResponse data = new DangNhapResponse(tk.getMaNguoiDung(), tk.getHoTen(), vaiTro, tk.isPhaiDoiMatKhau(),
				accessToken.hetHanLuc());
		// Không có transaction đang chạy (phiên đã commit trong taoPhien), nên ghi ngay
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_DANG_NHAP_THANH_CONG, tk.getMaNguoiDung(),
				DoiTuongTacDong.nguoiDung(tk.getMaNguoiDung()), new ChiTietPhien(maHoPhien)));
		return new KetQuaDangNhap(data, accessToken.value(), refreshToken, this.refreshTtl);
	}

	/** {@code maNguoiDung} null khi không xác định được tài khoản (spec 0007, AC-3). */
	private void ghiThatBai(String maNguoiDung, String lyDo) {
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_DANG_NHAP_THAT_BAI, maNguoiDung,
				DoiTuongTacDong.nguoiDung(maNguoiDung), new ChiTietDangNhapThatBai(lyDo)));
	}

	/** Lượt sai thứ N của tài khoản có thật vừa đặt khóa (AC-11, AC-17). */
	private void ghiKhoaTam(String maNguoiDung, Instant khoaDen) {
		log.info("Khóa tạm đăng nhập: maNguoiDung={}", maNguoiDung);
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_KHOA_TAM_DANG_NHAP, maNguoiDung,
				DoiTuongTacDong.nguoiDung(maNguoiDung), new ChiTietKhoaTam(khoaDen)));
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
