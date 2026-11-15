package vn.edu.hust.chungcu.common.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Nguồn giờ duy nhất của ứng dụng. Code lấy giờ qua bean {@link Clock}
 * (không gọi {@code Instant.now()} trực tiếp) để test đặt được giờ.
 * Múi giờ của Clock là {@code app.timezone}, nên {@code LocalDate.now(clock)}
 * cho ra ngày theo giờ Việt Nam; {@code Instant.now(clock)} vẫn là UTC.
 */
@Configuration(proxyBeanMethods = false)
public class TimeConfig {

	@Bean
	Clock clock(AppProperties properties) {
		return Clock.system(properties.timezone());
	}

}
