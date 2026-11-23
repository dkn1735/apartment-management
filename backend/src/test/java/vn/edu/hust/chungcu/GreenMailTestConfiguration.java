package vn.edu.hust.chungcu;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

/**
 * Máy chủ SMTP nhúng cho test gửi email (spec 0010). Một instance cổng ngẫu nhiên dùng
 * chung cho mọi context; mỗi test tự xóa hộp thư trước khi chạy.
 */
@TestConfiguration(proxyBeanMethods = false)
public class GreenMailTestConfiguration {

	private static final GreenMail GREEN_MAIL = batDau();

	private static GreenMail batDau() {
		GreenMail greenMail = new GreenMail(ServerSetupTest.SMTP.dynamicPort());
		greenMail.start();
		return greenMail;
	}

	// Không để Spring dừng máy chủ khi một context đóng: các context khác vẫn dùng
	@Bean(destroyMethod = "")
	GreenMail greenMail() {
		return GREEN_MAIL;
	}

	@Bean
	DynamicPropertyRegistrar mailProperties() {
		return (registry) -> {
			registry.add("spring.mail.host", () -> "127.0.0.1");
			registry.add("spring.mail.port", () -> GREEN_MAIL.getSmtp().getPort());
		};
	}

}
