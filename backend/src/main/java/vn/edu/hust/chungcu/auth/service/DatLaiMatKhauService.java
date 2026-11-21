package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.dto.DatLaiMatKhauResponse;

/**
 * Người quản lý đặt lại mật khẩu cho một tài khoản (spec 0008, AC-12). Sinh mật khẩu tạm
 * và băm bcrypt ngoài transaction; phần ghi ở {@link GhiDatLaiMatKhauService}.
 */
@Service
public class DatLaiMatKhauService {

	private final MatKhauTamGenerator matKhauTam;

	private final PasswordEncoder encoder;

	private final GhiDatLaiMatKhauService ghi;

	private final Clock clock;

	public DatLaiMatKhauService(MatKhauTamGenerator matKhauTam, PasswordEncoder encoder, GhiDatLaiMatKhauService ghi,
			Clock clock) {
		this.matKhauTam = matKhauTam;
		this.encoder = encoder;
		this.ghi = ghi;
		this.clock = clock;
	}

	public DatLaiMatKhauResponse datLai(NguoiQuanLy nguoi, String maNguoiDung) {
		if (maNguoiDung != null && maNguoiDung.equals(nguoi.maNguoiDung())) {
			throw TaiKhoanLoi.tuBaoVe("Bạn không thể tự đặt lại mật khẩu của mình. Hãy dùng trang Đổi mật khẩu.");
		}
		String matKhau = this.matKhauTam.sinh();
		String hash = this.encoder.encode(matKhau);
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		this.ghi.ghi(nguoi, maNguoiDung, hash, now);
		return new DatLaiMatKhauResponse(matKhau);
	}

}
