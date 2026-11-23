package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.TestcontainersConfiguration;
import vn.edu.hust.chungcu.auth.service.KhoaTamDangNhapService;
import vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator;

/** Job dọn chỉ xóa dòng định danh ảo đã hết tác dụng (spec 0009, AC-12). */
@Import({ TestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@ActiveProfiles("test")
class DangNhapSaiDonDepIntegrationTest {

	@Autowired
	KhoaTamDangNhapService khoaTam;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
	}

	@Test
	void chiXoaDongHetTacDung() {
		Instant now = this.clock.instant();
		them("cu", now.minus(Duration.ofMinutes(15)), null);
		them("cu-khoa-het-han", now.minus(Duration.ofMinutes(30)), now);
		them("moi", now.minus(Duration.ofMinutes(14)), null);
		them("dang-khoa", now.minus(Duration.ofMinutes(20)), now.plusSeconds(1));

		assertThat(this.khoaTam.donDep()).isEqualTo(2);
		assertThat(this.jdbc.queryForList("SELECT dinh_danh_hash FROM dang_nhap_sai_dinh_danh", String.class))
			.containsExactlyInAnyOrder(RefreshTokenGenerator.sha256Hex("moi"),
					RefreshTokenGenerator.sha256Hex("dang-khoa"));
	}

	private void them(String dinhDanh, Instant lanSaiCuoi, Instant khoaDen) {
		this.jdbc.update(
				"INSERT INTO dang_nhap_sai_dinh_danh (dinh_danh_hash, so_lan_sai, khoa_tam_den, lan_sai_cuoi_luc) VALUES (?, 1, ?, ?)",
				RefreshTokenGenerator.sha256Hex(dinhDanh), (khoaDen != null) ? Timestamp.from(khoaDen) : null,
				Timestamp.from(lanSaiCuoi));
	}

}
