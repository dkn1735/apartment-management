package vn.edu.hust.chungcu;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import vn.edu.hust.chungcu.common.config.AppProperties;

/**
 * Thay {@code Clock} hệ thống bằng {@link MutableClock} (cùng múi giờ
 * {@code app.timezone}).
 */
@TestConfiguration(proxyBeanMethods = false)
public class MutableClockConfiguration {

	@Bean
	@Primary
	MutableClock mutableClock(AppProperties properties) {
		return new MutableClock(MutableClock.MAC_DINH, properties.timezone());
	}

}
