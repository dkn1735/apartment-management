package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import vn.edu.hust.chungcu.MutableClock;
import vn.edu.hust.chungcu.MutableClockConfiguration;
import vn.edu.hust.chungcu.SeedTestcontainersConfiguration;

/**
 * Gửi song song không được quá N lượt so mật khẩu (spec 0009, AC-6), cho cả tài khoản
 * thật lẫn định danh ảo chưa có dòng.
 */
@Import({ SeedTestcontainersConfiguration.class, MutableClockConfiguration.class })
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "seed", "test" })
class KhoaTamSongSongIntegrationTest {

	private static final int SO_LUONG = 20;

	@Autowired
	MockMvc mockMvc;

	@Autowired
	MutableClock clock;

	@Autowired
	JdbcTemplate jdbc;

	@MockitoSpyBean
	PasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		KhoaTamTestSupport.datLai(this.jdbc);
		this.clock.reset();
	}

	@Test
	void taiKhoanThat() throws Exception {
		TaiKhoanThu.TaiKhoan tk = new TaiKhoanThu(this.jdbc).tao(this.passwordEncoder.encode(AuthApi.MAT_KHAU_SEED));
		clearInvocations(this.passwordEncoder);
		kiem(tk.soDienThoai());
		assertThat(KhoaTamTestSupport.cua(this.jdbc, tk.maNguoiDung()).khoaTamDen()).isNotNull();
	}

	@Test
	void dinhDanhAoChuaCoDong() throws Exception {
		String ao = TaiKhoanApi.soMoi();
		clearInvocations(this.passwordEncoder);
		kiem(ao);
		assertThat(KhoaTamTestSupport.dinhDanh(this.jdbc, ao).get("khoa_tam_den")).isNotNull();
	}

	private void kiem(String taiKhoan) throws Exception {
		RequestBuilder[] requests = IntStream.range(0, SO_LUONG)
			.mapToObj((i) -> AuthApi.loginRequest(taiKhoan, "SaiMatKhau" + i))
			.toArray(RequestBuilder[]::new);
		List<MvcResult> ketQua = AuthApi.songSong(this.mockMvc, requests);
		Map<Integer, Long> theoStatus = ketQua.stream()
			.collect(Collectors.groupingBy((r) -> r.getResponse().getStatus(), Collectors.counting()));
		assertThat(theoStatus.keySet()).containsOnly(401, 429);
		assertThat(theoStatus.getOrDefault(401, 0L)).isLessThanOrEqualTo(4);
		assertThat(theoStatus.getOrDefault(429, 0L)).isGreaterThanOrEqualTo(SO_LUONG - 4);
		// Cả khop lẫn soKhopGia đều đi qua PasswordEncoder.matches
		verify(this.passwordEncoder, atMost(5)).matches(any(), anyString());
	}

}
