package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Mật khẩu tạm (spec 0008, AC-2). */
class MatKhauTamGeneratorTest {

	private final MatKhauTamGenerator generator = new MatKhauTamGenerator();

	@Test
	void moiMatKhauDai12DungBangChuCoChuVaSo() {
		Set<String> daGap = new HashSet<>();
		for (int i = 0; i < 10_000; i++) {
			String mk = this.generator.sinh();
			assertThat(mk).hasSize(12);
			assertThat(mk.chars().allMatch((c) -> MatKhauTamGenerator.BANG_CHU.indexOf(c) >= 0)).as(mk).isTrue();
			assertThat(mk).as(mk).matches(".*[A-Za-z].*").matches(".*[0-9].*").doesNotContain("0", "O", "1", "l", "I");
			// Đạt quy tắc độ mạnh của DoiMatKhauRequest
			assertThat(mk).matches("^(?=.*\\p{L})(?=.*\\p{Nd}).*$");
			daGap.add(mk);
		}
		assertThat(daGap).hasSize(10_000);
	}

}
