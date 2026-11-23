package vn.edu.hust.chungcu.auth.service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
import vn.edu.hust.chungcu.common.config.AppProperties;
import vn.edu.hust.chungcu.common.email.EmailGui;

/**
 * Dựng email đặt lại mật khẩu từ text block, không template engine (spec 0010, Email).
 * Link là {@code app.public-url} cộng token trong fragment, không đọc header
 * {@code Host}. Họ tên được escape ở phần HTML; giờ hết hạn theo {@code app.timezone}.
 */
@Component
public class EmailDatLaiMatKhau {

	static final String TIEU_DE = "Đặt lại mật khẩu tài khoản chung cư";

	static final String DUONG_DAN = "/dat-lai-mat-khau#token=";

	private static final DateTimeFormatter GIO = DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

	private final String publicUrl;

	private final ZoneId timezone;

	public EmailDatLaiMatKhau(AppProperties properties) {
		this.publicUrl = properties.publicUrl();
		this.timezone = properties.timezone();
	}

	public EmailGui dung(YeuCauDatLaiDaTao suKien) {
		String link = this.publicUrl + DUONG_DAN + suKien.token();
		String han = GIO.withZone(this.timezone).format(suKien.hetHanLuc());
		String chuThuong = """
				Xin chào %s,

				Có người vừa yêu cầu đặt lại mật khẩu cho tài khoản chung cư của bạn.
				Mở link dưới đây để đặt mật khẩu mới:

				%s

				Link có hiệu lực tới %s và chỉ dùng được một lần.
				Nếu bạn không yêu cầu, hãy bỏ qua email này; mật khẩu của bạn không thay đổi.
				""".formatted(suKien.hoTen(), link, han);
		String linkHtml = HtmlUtils.htmlEscape(link);
		String html = """
				<!doctype html>
				<html lang="vi">
				<body style="font-family: Arial, sans-serif; color: #1f2937; line-height: 1.5;">
				<p>Xin chào %s,</p>
				<p>Có người vừa yêu cầu đặt lại mật khẩu cho tài khoản chung cư của bạn.</p>
				<p><a href="%s" style="display: inline-block; padding: 10px 16px; background: #1d4ed8; color: #ffffff; \
				text-decoration: none; border-radius: 6px;">Đặt lại mật khẩu</a></p>
				<p>Nếu nút không bấm được, hãy mở link này:<br><a href="%s">%s</a></p>
				<p>Link có hiệu lực tới %s và chỉ dùng được một lần.</p>
				<p>Nếu bạn không yêu cầu, hãy bỏ qua email này; mật khẩu của bạn không thay đổi.</p>
				</body>
				</html>
				""".formatted(HtmlUtils.htmlEscape(suKien.hoTen()), linkHtml, linkHtml, linkHtml, han);
		return new EmailGui(suKien.email(), TIEU_DE, chuThuong, html);
	}

}
