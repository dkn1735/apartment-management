package vn.edu.hust.chungcu.common.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import vn.edu.hust.chungcu.common.config.AppProperties;

/**
 * Gửi email qua SMTP bằng {@link JavaMailSender}: multipart UTF-8, có cả phần chữ thường
 * và phần HTML, người gửi là {@code app.mail.from} (spec 0010).
 */
@Component
public class SmtpEmailSender implements EmailSender {

	private final JavaMailSender mailSender;

	private final String nguoiGui;

	public SmtpEmailSender(JavaMailSender mailSender, AppProperties properties) {
		this.mailSender = mailSender;
		this.nguoiGui = properties.mail().from();
	}

	@Override
	public void gui(EmailGui email) {
		MimeMessage message = this.mailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
			helper.setFrom(this.nguoiGui);
			helper.setTo(email.nguoiNhan());
			helper.setSubject(email.tieuDe());
			helper.setText(email.chuThuong(), email.html());
		}
		catch (MessagingException ex) {
			throw new MailPreparationException(ex);
		}
		this.mailSender.send(message);
	}

}
