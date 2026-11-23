package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import vn.edu.hust.chungcu.common.config.AppProperties;

/** Token kế tiếp tất định theo khóa dẫn xuất, đúng định dạng (spec 0005, AC-5, AC-16). */
class RefreshTokenGeneratorTest {

	private final RefreshTokenGenerator generator = new RefreshTokenGenerator(cauHinh("k".repeat(32)));

	@Test
	void keTiepTatDinhVaDungDinhDang() {
		String t1 = this.generator.moi();
		String t2 = this.generator.keTiep(t1);
		assertThat(this.generator.keTiep(t1)).isEqualTo(t2);
		assertThat(new RefreshTokenGenerator(cauHinh("k".repeat(32))).keTiep(t1)).isEqualTo(t2);
		assertThat(t2).isNotEqualTo(t1).matches("[A-Za-z0-9_-]{43}");
		assertThat(RefreshTokenGenerator.dungDinhDang(t2)).isTrue();
	}

	@Test
	void doiJwtSecretThiKeTiepDoi() {
		String t1 = this.generator.moi();
		assertThat(new RefreshTokenGenerator(cauHinh("q".repeat(32))).keTiep(t1))
			.isNotEqualTo(this.generator.keTiep(t1));
	}

	@Test
	void moiVanNgauNhien() {
		assertThat(this.generator.moi()).isNotEqualTo(this.generator.moi()).matches("[A-Za-z0-9_-]{43}");
	}

	@Test
	void kiemDinhDang() {
		assertThat(RefreshTokenGenerator.dungDinhDang(null)).isFalse();
		assertThat(RefreshTokenGenerator.dungDinhDang("a".repeat(42))).isFalse();
		assertThat(RefreshTokenGenerator.dungDinhDang("a".repeat(44))).isFalse();
		assertThat(RefreshTokenGenerator.dungDinhDang("a".repeat(42) + "+")).isFalse();
		assertThat(RefreshTokenGenerator.dungDinhDang("a".repeat(42) + "-")).isTrue();
	}

	@Test
	void toStringKhongLoKhoa() {
		assertThat(this.generator.toString()).doesNotContain("k".repeat(8));
	}

	private static AppProperties cauHinh(String jwtSecret) {
		return new AppProperties(ZoneId.of("Asia/Ho_Chi_Minh"),
				new AppProperties.Security(jwtSecret, Duration.ofMinutes(15), Duration.ofDays(7), 12, true,
						Duration.ofDays(30), Duration.ofSeconds(30), Duration.ofDays(30), "0 0 3 * * *",
						Duration.ofMinutes(30)),
				"http://localhost:3000", new AppProperties.Mail("chungcu@localhost"));
	}

}
