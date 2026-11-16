package vn.edu.hust.chungcu;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Container riêng cho các test bật profile {@code seed}. Tách khỏi
 * {@link TestcontainersConfiguration} để database đã seed không lọt sang các test kỳ vọng
 * database trống (AC-7), bất kể thứ tự chạy test.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SeedTestcontainersConfiguration {

	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
			TestcontainersConfiguration.POSTGRES_IMAGE);

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return POSTGRES;
	}

}
