package vn.edu.hust.chungcu.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import vn.edu.hust.chungcu.auth.service.AuthCookies;

/**
 * Backend không khởi động khi {@code JWT_SECRET} sai; cờ {@code Secure} theo cấu hình
 * (AC-11).
 */
class AppPropertiesTest {

	private static final String KHOA_HOP_LE = "app.security.jwt-secret=" + "k".repeat(32);

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(ValidationAutoConfiguration.class))
		.withUserConfiguration(Config.class)
		.withPropertyValues("app.timezone=Asia/Ho_Chi_Minh", "app.security.access-token-ttl=PT15M",
				"app.security.refresh-token-ttl=P7D", "app.security.bcrypt-cost=12", "app.security.cookie-secure=true");

	@Test
	void thieuJwtSecretThiKhongKhoiDong() {
		this.runner.run(
				context -> assertThat(context).hasFailed().getFailure().rootCause().hasMessageContaining("JWT_SECRET"));
		this.runner.withPropertyValues("app.security.jwt-secret=")
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("JWT_SECRET"));
	}

	@Test
	void jwtSecret31ByteThiKhongKhoiDong() {
		this.runner.withPropertyValues("app.security.jwt-secret=" + "k".repeat(31))
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("JWT_SECRET phải dài ít nhất 32 byte"));
	}

	@Test
	void jwtSecretDemTheoByteUtf8() {
		// 11 ký tự "ệ" là 33 byte UTF-8
		this.runner.withPropertyValues("app.security.jwt-secret=" + "ệ".repeat(11))
			.run(context -> assertThat(context).hasNotFailed());
	}

	@Test
	void jwtSecretLaChuoiMauThiKhongKhoiDong() {
		this.runner.withPropertyValues("app.security.jwt-secret=" + AppProperties.Security.JWT_SECRET_MAU)
			.run(context -> assertThat(context).hasFailed()
				.getFailure()
				.rootCause()
				.hasMessageContaining("JWT_SECRET đang là chuỗi mẫu"));
	}

	@Test
	void cookieSecureMacDinhCoCoSecure() {
		this.runner.withPropertyValues(KHOA_HOP_LE).withBean(AuthCookies.class).run(context -> {
			assertThat(context.getBean(AuthCookies.class).dat("a", "r").get("Set-Cookie")).hasSize(3)
				.allSatisfy(cookie -> assertThat(cookie).contains("; Secure"));
		});
	}

	@Test
	void cookieSecureFalseThiKhongCoCoSecure() {
		this.runner.withPropertyValues(KHOA_HOP_LE, "app.security.cookie-secure=false")
			.withBean(AuthCookies.class)
			.run(context -> {
				assertThat(context.getBean(AuthCookies.class).dat("a", "r").get("Set-Cookie")).hasSize(3)
					.allSatisfy(cookie -> assertThat(cookie).doesNotContain("Secure"));
				assertThat(context.getBean(AuthCookies.class).xoa().get("Set-Cookie")).hasSize(3)
					.allSatisfy(cookie -> assertThat(cookie).contains("Max-Age=0"));
			});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(AppProperties.class)
	static class Config {

	}

}
