package vn.edu.hust.chungcu.canho;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import jakarta.servlet.http.Cookie;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * Dựng request cho API căn hộ (spec 0012) trong test. Căn test nằm ở tòa test (mã bắt đầu
 * bằng {@code T}), nên {@link ToaNhaApi#donDep} xóa cả căn; căn seed chỉ đọc.
 */
final class CanHoApi {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private CanHoApi() {
	}

	static MockHttpServletRequestBuilder tim(String token, String query) {
		return get("/api/v1/apartments" + query).cookie(cookie(token));
	}

	static MockHttpServletRequestBuilder chiTiet(String token, String ma) {
		return get("/api/v1/apartments/" + ma).cookie(cookie(token));
	}

	static Map<String, Object> body(Object maTang, Object soCan, Object dienTich) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("maTang", maTang);
		body.put("soCan", soCan);
		body.put("dienTich", dienTich);
		return body;
	}

	static MockHttpServletRequestBuilder them(String token, Object body) {
		return themRaw(token, JSON.writeValueAsString(body));
	}

	static MockHttpServletRequestBuilder themRaw(String token, String body) {
		return json(post("/api/v1/apartments"), token, body);
	}

	static MockHttpServletRequestBuilder sua(String token, String ma, Object dienTich, Object trangThai) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("dienTich", dienTich);
		body.put("trangThaiSuDung", trangThai);
		return suaRaw(token, ma, JSON.writeValueAsString(body));
	}

	static MockHttpServletRequestBuilder suaRaw(String token, String ma, String body) {
		return json(put("/api/v1/apartments/" + ma), token, body);
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
