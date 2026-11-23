package vn.edu.hust.chungcu.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * Link đặt lại bị hủy khi mật khẩu đổi, tài khoản bị khóa hay email bị sửa (spec 0010,
 * AC-10), và các luồng đó chạy song song với yêu cầu, xác nhận không deadlock.
 */
class QuenMatKhauHuyIntegrationTest extends QuenMatKhauTestBase {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	String admin;

	String ma;

	String email;

	@BeforeEach
	void taoTaiKhoanQuaApi() throws Exception {
		this.admin = this.auth.accessToken(TaiKhoanApi.ADMIN);
		Map<String, Object> body = TaiKhoanApi.cuDanMoi();
		this.mockMvc.perform(TaiKhoanApi.tao(this.admin, body)).andExpect(status().isCreated());
		this.ma = TaiKhoanApi.maTheoSo(this.jdbc, (String) body.get("soDienThoai"));
		this.email = (String) body.get("email");
	}

	String yeuCau() throws Exception {
		this.clock.advance(Duration.ofSeconds(61));
		this.mockMvc.perform(QuenMatKhauThu.yeuCau(this.email)).andExpect(status().isOk());
		return QuenMatKhauThu.tokenMoiNhat(this.greenMail, this.jdbc, this.ma);
	}

	void phaiBiHuy(String token) throws Exception {
		this.mockMvc.perform(QuenMatKhauThu.kiemTra(token)).andExpect(status().isBadRequest());
		List<Map<String, Object>> dong = QuenMatKhauThu.yeuCauCua(this.jdbc, this.ma);
		assertThat(dong.get(dong.size() - 1).get("huy_luc")).isNotNull();
	}

	long phienBan() {
		Long v = this.jdbc.queryForObject("SELECT phien_ban FROM nguoi_dung_he_thong WHERE ma_nguoi_dung = ?",
				Long.class, this.ma);
		return (v != null) ? v : -1;
	}

	@Test
	void adminDatLaiMatKhauHuyLink() throws Exception {
		String token = yeuCau();
		this.mockMvc.perform(TaiKhoanApi.datLai(this.admin, this.ma)).andExpect(status().isOk());
		phaiBiHuy(token);
	}

	@Test
	void khoaTaiKhoanHuyLink() throws Exception {
		String token = yeuCau();
		this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, this.ma, "Tạm khóa", "Thử"))
			.andExpect(status().isOk());
		phaiBiHuy(token);
	}

	@Test
	void suaEmailHuyLinkConGiuEmailThiKhong() throws Exception {
		String token = yeuCau();
		Map<String, Object> chiTiet = chiTiet();
		this.mockMvc
			.perform(TaiKhoanApi.sua(this.admin, this.ma, Map.of("hoTen", "Tên Mới", "soDienThoai",
					chiTiet.get("soDienThoai"), "email", this.email, "phienBan", phienBan())))
			.andExpect(status().isOk());
		this.mockMvc.perform(QuenMatKhauThu.kiemTra(token)).andExpect(status().isOk());

		this.mockMvc
			.perform(TaiKhoanApi.sua(this.admin, this.ma, Map.of("hoTen", "Tên Mới", "soDienThoai",
					chiTiet.get("soDienThoai"), "email", "moi" + this.email, "phienBan", phienBan())))
			.andExpect(status().isOk());
		phaiBiHuy(token);
	}

	@Test
	void tuDoiMatKhauHuyLink() throws Exception {
		// Đặt mật khẩu biết trước qua chính link, rồi đăng nhập và tự đổi
		String dau = yeuCau();
		this.mockMvc.perform(QuenMatKhauThu.xacNhan(dau, MAT_KHAU_MOI)).andExpect(status().isOk());
		String token = yeuCau();
		String access = AuthApi.cookieValue(this.auth.login(this.email, MAT_KHAU_MOI), "access_token").orElseThrow();
		this.mockMvc
			.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.put("/api/v1/auth/change-password")
				.header("X-Requested-With", "XMLHttpRequest")
				.cookie(new Cookie("access_token", access))
				.contentType(MediaType.APPLICATION_JSON)
				.content(JSON.writeValueAsString(Map.of("matKhauCu", MAT_KHAU_MOI, "matKhauMoi", "MatKhauKhac2026"))))
			.andExpect(status().isOk());
		phaiBiHuy(token);
	}

	@Test
	void yeuCauXacNhanVaKhoaSongSongKhongDeadlock() throws Exception {
		for (int lan = 0; lan < 5; lan++) {
			// Mỗi vòng tạo tới hai yêu cầu; dời giờ để không chạm giới hạn 5 lần mỗi giờ
			this.clock.advance(Duration.ofMinutes(30));
			this.mockMvc.perform(TaiKhoanApi.doiTrangThai(this.admin, this.ma, "Hoạt động", null));
			String token = yeuCau();
			this.clock.advance(Duration.ofSeconds(61));
			List<MvcResult> ketQua = AuthApi.songSong(this.mockMvc, QuenMatKhauThu.yeuCau(this.email),
					QuenMatKhauThu.xacNhan(token, MAT_KHAU_MOI),
					TaiKhoanApi.doiTrangThai(this.admin, this.ma, "Tạm khóa", "Thử song song"));
			for (MvcResult r : ketQua) {
				assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isLessThan(500);
			}
		}
	}

	@SuppressWarnings("unchecked")
	Map<String, Object> chiTiet() throws Exception {
		String body = this.mockMvc.perform(TaiKhoanApi.chiTiet(this.admin, this.ma))
			.andReturn()
			.getResponse()
			.getContentAsString();
		return (Map<String, Object>) JSON.readValue(body, Map.class).get("data");
	}

}
