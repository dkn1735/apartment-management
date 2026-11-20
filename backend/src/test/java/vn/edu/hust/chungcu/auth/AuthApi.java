package vn.edu.hust.chungcu.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/** Gọi API xác thực trong test qua MockMvc, có sẵn header chống CSRF. */
public final class AuthApi {

	public static final String MAT_KHAU_SEED = "ChungCu@2026";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final MockMvc mockMvc;

	public AuthApi(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	public static MockHttpServletRequestBuilder loginRequest(String taiKhoan, String matKhau) {
		return post("/api/v1/auth/login").header("X-Requested-With", "XMLHttpRequest")
			.contentType(MediaType.APPLICATION_JSON)
			.content(JSON.writeValueAsString(new Body(taiKhoan, matKhau)));
	}

	public MvcResult login(String taiKhoan, String matKhau) throws Exception {
		return this.mockMvc.perform(loginRequest(taiKhoan, matKhau)).andReturn();
	}

	/** Đăng nhập rồi trả giá trị cookie {@code access_token}. */
	public String accessToken(String taiKhoan) throws Exception {
		MvcResult result = login(taiKhoan, MAT_KHAU_SEED);
		return cookieValue(result, "access_token")
			.orElseThrow(() -> new AssertionError("Đăng nhập thất bại: " + result.getResponse().getStatus()));
	}

	public MockHttpServletRequestBuilder profile(String accessToken) {
		return get("/api/v1/auth/profile").cookie(new Cookie("access_token", accessToken));
	}

	/** Làm mới phiên (spec 0005); {@code refreshToken} null thì không gửi cookie. */
	public static MockHttpServletRequestBuilder refreshRequest(String refreshToken) {
		MockHttpServletRequestBuilder request = post("/api/v1/auth/refresh-token").header("X-Requested-With",
				"XMLHttpRequest");
		return (refreshToken != null) ? request.cookie(new Cookie("refresh_token", refreshToken)) : request;
	}

	/** Đăng xuất (spec 0005); {@code refreshToken} null thì không gửi cookie. */
	public static MockHttpServletRequestBuilder logoutRequest(String refreshToken) {
		MockHttpServletRequestBuilder request = post("/api/v1/auth/logout").header("X-Requested-With",
				"XMLHttpRequest");
		return (refreshToken != null) ? request.cookie(new Cookie("refresh_token", refreshToken)) : request;
	}

	public MvcResult refresh(String refreshToken) throws Exception {
		return this.mockMvc.perform(refreshRequest(refreshToken)).andReturn();
	}

	/** Đăng nhập rồi trả giá trị cookie {@code refresh_token}. */
	public String refreshToken(String taiKhoan) throws Exception {
		MvcResult result = login(taiKhoan, MAT_KHAU_SEED);
		return cookieValue(result, "refresh_token")
			.orElseThrow(() -> new AssertionError("Đăng nhập thất bại: " + result.getResponse().getStatus()));
	}

	/** Chạy các request cùng lúc nhất có thể, trả kết quả theo đúng thứ tự truyền vào. */
	public static List<MvcResult> songSong(MockMvc mockMvc, RequestBuilder... requests) throws Exception {
		CountDownLatch batDau = new CountDownLatch(1);
		try (ExecutorService pool = Executors.newFixedThreadPool(requests.length)) {
			List<Future<MvcResult>> futures = Arrays.stream(requests).map(r -> pool.submit(() -> {
				batDau.await();
				return mockMvc.perform(r).andReturn();
			})).toList();
			batDau.countDown();
			List<MvcResult> ketQua = new ArrayList<>();
			for (Future<MvcResult> f : futures) {
				ketQua.add(f.get());
			}
			return ketQua;
		}
	}

	public static List<String> setCookies(MvcResult result) {
		return result.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
	}

	public static Optional<String> setCookie(MvcResult result, String name) {
		return setCookies(result).stream().filter(header -> header.startsWith(name + "=")).findFirst();
	}

	public static Optional<String> cookieValue(MvcResult result, String name) {
		return setCookie(result, name).map(header -> header.substring(name.length() + 1, header.indexOf(';')));
	}

	record Body(String taiKhoan, String matKhau) {
	}

}
