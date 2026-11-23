package vn.edu.hust.chungcu.canho;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.Cookie;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dựng request cho API tòa nhà, tầng, khu vực dùng chung (spec 0011) trong test. Database
 * seed dùng chung giữa các lớp test, nên mọi tòa tạo ra có mã bắt đầu bằng {@code T} và
 * được {@link #donDep} xóa sau mỗi test.
 */
final class ToaNhaApi {

	static final String BQL = "0900000002";

	static final String KY_THUAT = "0900000003";

	static final String KE_TOAN = "0900000004";

	static final String BQT = "0900000006";

	static final String CU_DAN = "0900000007";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final AtomicInteger DEM = new AtomicInteger();

	private ToaNhaApi() {
	}

	/** Mã tòa chưa dùng, dạng {@code T<số>}. */
	static String maMoi() {
		return "T" + (System.nanoTime() % 100_000) + DEM.incrementAndGet();
	}

	/**
	 * Xóa mọi tòa test (mã bắt đầu bằng {@code T}) cùng tầng của nó; tòa seed giữ nguyên.
	 */
	static void donDep(JdbcTemplate jdbc) {
		jdbc.update("DELETE FROM tang WHERE ma_toa_nha LIKE 'T%'");
		jdbc.update("DELETE FROM toa_nha WHERE ma_toa_nha LIKE 'T%'");
	}

	static Map<String, Object> body(Object ma, Object ten, Object soTang, Object diaChi) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("maToaNha", ma);
		body.put("tenToaNha", ten);
		body.put("soTang", soTang);
		body.put("diaChiKhuVuc", diaChi);
		return body;
	}

	static Map<String, Object> toaMoi(String ma, int soTang) {
		return body(ma, "Tòa thử " + ma, soTang, "Khu thử nghiệm");
	}

	static MockHttpServletRequestBuilder themToa(String token, Object body) {
		return json(post("/api/v1/buildings"), token, JSON.writeValueAsString(body));
	}

	static MockHttpServletRequestBuilder themToaRaw(String token, String body) {
		return json(post("/api/v1/buildings"), token, body);
	}

	static MockHttpServletRequestBuilder themTang(String token, String maToaNha, Object soTang) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("soTang", soTang);
		return json(post("/api/v1/buildings/" + maToaNha + "/floors"), token, JSON.writeValueAsString(body));
	}

	static MockHttpServletRequestBuilder chiTiet(String token, String maToaNha) {
		return get("/api/v1/buildings/" + maToaNha).cookie(cookie(token));
	}

	static MockHttpServletRequestBuilder dsTang(String token, String maToaNha, String query) {
		return get("/api/v1/buildings/" + maToaNha + "/floors" + query).cookie(cookie(token));
	}

	static MockHttpServletRequestBuilder khuVuc(String token, String query) {
		return get("/api/v1/common-areas" + query).cookie(cookie(token));
	}

	private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder r, String token, String body) {
		return r.header("X-Requested-With", "XMLHttpRequest")
			.cookie(cookie(token))
			.contentType(MediaType.APPLICATION_JSON)
			.content(body);
	}

	private static Cookie cookie(String token) {
		return new Cookie("access_token", token);
	}

}
