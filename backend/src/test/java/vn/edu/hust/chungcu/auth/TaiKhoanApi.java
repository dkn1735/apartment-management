package vn.edu.hust.chungcu.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dựng request cho API quản lý tài khoản (spec 0008) trong test, có sẵn header chống CSRF
 * và cookie access token.
 */
public final class TaiKhoanApi {

	public static final String ADMIN = "0900000001";

	public static final String BQL = "0900000002";

	public static final String KY_THUAT = "0900000003";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private TaiKhoanApi() {
	}

	/** Số điện thoại ngẫu nhiên không đụng dải seed {@code 09000000xx}. */
	public static String soMoi() {
		return "093" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
	}

	/** Mã người dùng của một số điện thoại. */
	public static String maTheoSo(JdbcTemplate jdbc, String soDienThoai) {
		return jdbc.queryForObject("SELECT ma_nguoi_dung FROM nguoi_dung_he_thong WHERE so_dien_thoai = ?",
				String.class, soDienThoai);
	}

	/** Số CCCD ngẫu nhiên 12 chữ số. */
	public static String cccdMoi() {
		return String.format("%012d", ThreadLocalRandom.current().nextLong(1_000_000_000_000L));
	}

	/** Body tạo tài khoản; {@code vaiTro} là các map đã có khóa {@code vaiTro}. */
	public static Map<String, Object> body(String hoTen, String soDienThoai, String email,
			List<Map<String, Object>> vaiTro) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("hoTen", hoTen);
		body.put("soDienThoai", soDienThoai);
		body.put("email", email);
		body.put("vaiTro", vaiTro);
		return body;
	}

	/** Một phần tử vai trò: mã cộng các cặp tên trường, giá trị. */
	public static Map<String, Object> vaiTro(String ma, String... truongGiaTri) {
		Map<String, Object> v = new LinkedHashMap<>();
		v.put("vaiTro", ma);
		for (int i = 0; i < truongGiaTri.length; i += 2) {
			v.put(truongGiaTri[i], truongGiaTri[i + 1]);
		}
		return v;
	}

	/** Body hợp lệ có một vai trò cư dân, số điện thoại, email, CCCD ngẫu nhiên. */
	public static Map<String, Object> cuDanMoi() {
		String so = soMoi();
		return body("Cư Dân Thử", so, "cd" + so + "@chungcu.test",
				new ArrayList<>(List.of(vaiTro("CU_DAN", "soCccd", cccdMoi()))));
	}

	public static MockHttpServletRequestBuilder tao(String accessToken, Object body) {
		return json(post("/api/v1/accounts"), accessToken, body);
	}

	public static MockHttpServletRequestBuilder danhSach(String accessToken) {
		return get("/api/v1/accounts").cookie(new Cookie("access_token", accessToken));
	}

	public static MockHttpServletRequestBuilder chiTiet(String accessToken, String ma) {
		return get("/api/v1/accounts/" + ma).cookie(new Cookie("access_token", accessToken));
	}

	public static MockHttpServletRequestBuilder sua(String accessToken, String ma, Object body) {
		return json(put("/api/v1/accounts/" + ma), accessToken, body);
	}

	public static MockHttpServletRequestBuilder suaVaiTro(String accessToken, String ma, String vaiTro, Object body) {
		return json(put("/api/v1/accounts/" + ma + "/roles/" + vaiTro), accessToken, body);
	}

	public static MockHttpServletRequestBuilder goVaiTro(String accessToken, String ma, String vaiTro,
			Object phienBan) {
		MockHttpServletRequestBuilder r = delete("/api/v1/accounts/" + ma + "/roles/" + vaiTro)
			.header("X-Requested-With", "XMLHttpRequest")
			.cookie(new Cookie("access_token", accessToken));
		return (phienBan != null) ? r.param("phienBan", phienBan.toString()) : r;
	}

	public static MockHttpServletRequestBuilder doiTrangThai(String accessToken, String ma, String trangThai,
			String lyDo) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("trangThai", trangThai);
		body.put("lyDo", lyDo);
		return json(put("/api/v1/accounts/" + ma + "/status"), accessToken, body);
	}

	public static MockHttpServletRequestBuilder datLai(String accessToken, String ma) {
		return post("/api/v1/accounts/" + ma + "/reset-password").header("X-Requested-With", "XMLHttpRequest")
			.cookie(new Cookie("access_token", accessToken));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, String accessToken,
			Object body) {
		return r.header("X-Requested-With", "XMLHttpRequest")
			.cookie(new Cookie("access_token", accessToken))
			.contentType(MediaType.APPLICATION_JSON)
			.content(JSON.writeValueAsString(body));
	}

}
