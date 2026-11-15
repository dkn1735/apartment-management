package vn.edu.hust.chungcu;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-not-for-real-use")
// Ghi đè SPRING_PROFILES_ACTIVE trong backend/.env, để test không bao giờ nạp seed
@ActiveProfiles("dev")
class ChungCuApplicationTests {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void contextLoads() {
	}

	@Test
	void khongCoSeedKhiKhongBatProfileSeed() {
		Integer soTaiKhoan = this.jdbc.queryForObject("SELECT count(*) FROM nguoi_dung_he_thong", Integer.class);
		assertThat(soTaiKhoan).isZero();
	}

}
