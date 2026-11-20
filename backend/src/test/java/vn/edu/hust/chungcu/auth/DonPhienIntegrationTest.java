package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.service.PhienDangNhapCleanupJob;

/** Job dọn phiên của spec 0005 (AC-15, AC-16). */
// Dữ liệu được commit, nên dùng container seed như các test xác thực khác (database trống
// của TestcontainersConfiguration phải giữ trống)
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@ActiveProfiles({ "dev", "seed", "test" })
@ExtendWith(OutputCaptureExtension.class)
class DonPhienIntegrationTest {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	MutableClock clock;

	@Autowired
	PhienDangNhapCleanupJob job;

	@Autowired
	PasswordEncoder passwordEncoder;

	String maNguoiDung;

	@BeforeEach
	void setUp() {
		this.clock.reset();
		this.maNguoiDung = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode("x")).maNguoiDung();
	}

	@Test
	void chiXoaDongHetHanHoacThuHoiQuaBaMuoiNgay(CapturedOutput output) {
		Instant now = this.clock.instant();
		Instant ngay = now.minus(Duration.ofDays(40));
		String hetHan31 = chen(ngay, now.minus(Duration.ofDays(31)), null);
		String thuHoi31 = chen(now.minus(Duration.ofDays(35)), now.plus(Duration.ofDays(1)),
				now.minus(Duration.ofDays(31)));
		String thuHoi29 = chen(now.minus(Duration.ofDays(30)), now.plus(Duration.ofDays(1)),
				now.minus(Duration.ofDays(29)));
		String conHieuLuc = chen(now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(6)), null);

		this.job.chay();

		assertThat(this.jdbc.queryForList("SELECT token_hash FROM phien_dang_nhap WHERE ma_nguoi_dung = ?",
				String.class, this.maNguoiDung))
			.containsExactlyInAnyOrder(thuHoi29, conHieuLuc)
			.doesNotContain(hetHan31, thuHoi31);
		assertThat(output.getAll()).contains("Dọn phiên: daXoa=2").doesNotContain(thuHoi29).doesNotContain(conHieuLuc);
	}

	/** Chèn một dòng với hash ngẫu nhiên; trả hash. */
	private String chen(Instant taoLuc, Instant hetHanLuc, Instant thuHoiLuc) {
		String hash = (UUID.randomUUID().toString() + UUID.randomUUID()).replace("-", "").substring(0, 64);
		this.jdbc.update("""
				INSERT INTO phien_dang_nhap (ma_nguoi_dung, ma_ho_phien, token_hash, bat_dau_luc, tao_luc, het_han_luc,
				    thu_hoi_luc, ly_do_thu_hoi)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?)""", this.maNguoiDung, UUID.randomUUID(), hash, Timestamp.from(taoLuc),
				Timestamp.from(taoLuc), Timestamp.from(hetHanLuc),
				(thuHoiLuc != null) ? Timestamp.from(thuHoiLuc) : null, (thuHoiLuc != null) ? "Đăng xuất" : null);
		return hash;
	}

}
