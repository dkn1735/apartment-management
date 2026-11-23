package vn.edu.hust.chungcu.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.mail.autoconfigure.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Profile {@code prod} thiếu {@code MAIL_HOST} thì backend dừng lúc khởi động, thông báo
 * nêu tên biến (spec 0010, AC-14). Thiếu hẳn {@code spring.mail.host} thì Spring Boot chỉ
 * lặng lẽ không tạo {@code JavaMailSender}, nên cấu hình prod phải là placeholder không
 * có giá trị mặc định.
 */
class MailCauHinhProdTest {

	@Test
	void prodDungPlaceholderMailHostKhongMacDinh() throws Exception {
		List<PropertySource<?>> nguon = new YamlPropertySourceLoader().load("prod",
				new ClassPathResource("application-prod.yml"));
		assertThat(nguon).singleElement()
			.extracting(n -> n.getProperty("spring.mail.host"))
			.hasToString("${MAIL_HOST}");
	}

	@Test
	void thieuMailHostThiKhongKhoiDong() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
			.withPropertyValues("spring.mail.host=${MAIL_HOST}")
			.run(context -> assertThat(context).hasFailed().getFailure().rootCause().hasMessageContaining("MAIL_HOST"));
	}

}
