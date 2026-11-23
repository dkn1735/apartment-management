package vn.edu.hust.chungcu.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.icegreen.greenmail.util.GreenMail;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMessage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

/** Gọi API quên mật khẩu (spec 0010) và đọc email trong GreenMail cho test. */
public final class QuenMatKhauThu {

	public static final String PHAN_HOI_CHUNG = "Nếu email này thuộc một tài khoản đang hoạt động, link đặt lại mật khẩu sẽ được gửi tới trong vài phút.";

	public static final String TOKEN_HONG = "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu link mới.";

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final Pattern LINK = Pattern
		.compile("http://localhost:3000/dat-lai-mat-khau#token=([A-Za-z0-9_-]{43})");

	private QuenMatKhauThu() {
	}

	public static MockHttpServletRequestBuilder yeuCau(String email) {
		return json("/api/v1/auth/password-reset/request", Map.of("email", email));
	}

	public static MockHttpServletRequestBuilder kiemTra(String token) {
		return json("/api/v1/auth/password-reset/check", Map.of("token", token));
	}

	public static MockHttpServletRequestBuilder xacNhan(String token, String matKhauMoi) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("token", token);
		body.put("matKhauMoi", matKhauMoi);
		return json("/api/v1/auth/password-reset/confirm", body);
	}

	private static MockHttpServletRequestBuilder json(String path, Object body) {
		return post(path).header("X-Requested-With", "XMLHttpRequest")
			.contentType(MediaType.APPLICATION_JSON)
			.content(JSON.writeValueAsString(body));
	}

	/**
	 * Xóa mọi yêu cầu và hộp thư, để giới hạn chung không dồn từ test này sang test khác.
	 */
	public static void datLai(JdbcTemplate jdbc, GreenMail greenMail) throws Exception {
		jdbc.update("DELETE FROM yeu_cau_dat_lai_mat_khau");
		greenMail.purgeEmailFromAllMailboxes();
	}

	/** Chờ đúng {@code soEmail} email, rồi trả chúng. */
	public static MimeMessage[] choEmail(GreenMail greenMail, int soEmail) {
		if (!greenMail.waitForIncomingEmail(10_000, soEmail)) {
			throw new AssertionError(
					"Không nhận đủ " + soEmail + " email, chỉ có " + greenMail.getReceivedMessages().length);
		}
		return greenMail.getReceivedMessages();
	}

	/**
	 * Token của email ứng với dòng yêu cầu mới nhất của tài khoản. Tìm theo hash, không
	 * theo thứ tự: email của test trước có thể tới muộn sau khi hộp thư đã được xóa.
	 */
	public static String tokenMoiNhat(GreenMail greenMail, JdbcTemplate jdbc, String maNguoiDung) throws Exception {
		List<Map<String, Object>> dong = yeuCauCua(jdbc, maNguoiDung);
		String hash = (String) dong.get(dong.size() - 1).get("token_hash");
		long han = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < han) {
			for (MimeMessage m : greenMail.getReceivedMessages()) {
				String token = token(m);
				if (vn.edu.hust.chungcu.auth.service.RefreshTokenGenerator.sha256Hex(token).equals(hash)) {
					return token;
				}
			}
			greenMail.waitForIncomingEmail(200, greenMail.getReceivedMessages().length + 1);
		}
		throw new AssertionError("Không thấy email của yêu cầu mới nhất");
	}

	/** Phần chữ thường và phần HTML của email, đã giải mã. */
	public record NoiDung(String chuThuong, String html) {
	}

	public static NoiDung noiDung(MimeMessage message) throws Exception {
		String[] phan = new String[2];
		doc(message, phan);
		return new NoiDung(phan[0], phan[1]);
	}

	private static void doc(Part part, String[] phan) throws Exception {
		if (part.isMimeType("text/plain") && phan[0] == null) {
			phan[0] = (String) part.getContent();
		}
		else if (part.isMimeType("text/html") && phan[1] == null) {
			phan[1] = (String) part.getContent();
		}
		else if (part.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) part.getContent();
			for (int i = 0; i < multipart.getCount(); i++) {
				BodyPart con = multipart.getBodyPart(i);
				doc(con, phan);
			}
		}
	}

	/** Token trong link của phần chữ thường; phần HTML phải có cùng link. */
	public static String token(MimeMessage message) throws Exception {
		NoiDung noiDung = noiDung(message);
		String token = tim(noiDung.chuThuong());
		if (!token.equals(tim(noiDung.html()))) {
			throw new AssertionError("Phần HTML và phần chữ thường có link khác nhau");
		}
		return token;
	}

	private static String tim(String text) {
		Matcher m = LINK.matcher(text);
		if (!m.find()) {
			throw new AssertionError("Không thấy link đặt lại trong email");
		}
		return m.group(1);
	}

	/**
	 * Log của ứng dụng, bỏ dòng của chính GreenMail (nó ghi địa chỉ hộp thư mà nó tạo).
	 */
	public static String logUngDung(String output) {
		return output.lines()
			.filter(l -> !l.contains("c.icegreen."))
			.collect(java.util.stream.Collectors.joining(System.lineSeparator()));
	}

	public static List<Map<String, Object>> yeuCauCua(JdbcTemplate jdbc, String maNguoiDung) {
		return jdbc.queryForList("SELECT * FROM yeu_cau_dat_lai_mat_khau WHERE ma_nguoi_dung = ? ORDER BY ma_yeu_cau",
				maNguoiDung);
	}

}
