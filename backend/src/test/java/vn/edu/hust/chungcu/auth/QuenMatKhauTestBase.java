package vn.edu.hust.chungcu.auth;

import com.icegreen.greenmail.util.GreenMail;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.hust.chungcu.GreenMailTestConfiguration;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;

/**
 * Lớp gốc của integration test quên mật khẩu (spec 0010): PostgreSQL đã seed, đồng hồ
 * chỉnh được và GreenMail làm máy chủ SMTP. Mỗi test bắt đầu với bảng yêu cầu và hộp thư
 * trống, và một tài khoản riêng đang hoạt động có mật khẩu {@link AuthApi#MAT_KHAU_SEED}.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class, GreenMailTestConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
abstract class QuenMatKhauTestBase {

	static final String MAT_KHAU_MOI = "MatKhauMoi2026";

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	GreenMail greenMail;

	AuthApi auth;

	TaiKhoanThu taiKhoan;

	TaiKhoanThu.TaiKhoan tk;

	@BeforeEach
	void setUpQuenMatKhau() throws Exception {
		KhoaTamTestSupport.datLai(this.jdbc);
		QuenMatKhauThu.datLai(this.jdbc, this.greenMail);
		this.clock.reset();
		this.auth = new AuthApi(this.mockMvc);
		this.taiKhoan = new TaiKhoanThu(this.jdbc);
		this.tk = this.taiKhoan.tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
	}

	/**
	 * Yêu cầu link cho {@link #tk} rồi trả token trong email thứ {@code thuThu} (đếm từ
	 * 1).
	 */
	String yeuCauVaLayToken(int thuThu) throws Exception {
		this.mockMvc.perform(QuenMatKhauThu.yeuCau(this.tk.email()));
		QuenMatKhauThu.choEmail(this.greenMail, thuThu);
		return QuenMatKhauThu.tokenMoiNhat(this.greenMail, this.jdbc, this.tk.maNguoiDung());
	}

	int soYeuCau() {
		Integer so = this.jdbc.queryForObject("SELECT count(*) FROM yeu_cau_dat_lai_mat_khau WHERE ma_nguoi_dung = ?",
				Integer.class, this.tk.maNguoiDung());
		return (so != null) ? so : 0;
	}

	/** Status, body và header của response, bỏ các giá trị đổi theo từng request. */
	static Map<String, Object> phanHoi(MvcResult result) {
		Map<String, Object> headers = new java.util.TreeMap<>();
		for (String ten : result.getResponse().getHeaderNames()) {
			if (!ten.equalsIgnoreCase("X-Request-Id") && !ten.equalsIgnoreCase("Date")) {
				headers.put(ten.toLowerCase(java.util.Locale.ROOT), result.getResponse().getHeaders(ten));
			}
		}
		try {
			// timestamp của vỏ bọc lấy giờ hệ thống, khác nhau ở mỗi response
			String body = result.getResponse().getContentAsString().replaceAll("\"timestamp\":\"[^\"]*\"", "");
			return Map.of("status", result.getResponse().getStatus(), "body", body, "headers", headers);
		}
		catch (java.io.UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
