package vn.edu.hust.chungcu;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Container PostgreSQL dùng chung cho mọi test không bật profile {@code seed}. Mỗi bộ
 * profile tạo một Spring context riêng, nhưng các context này cùng trỏ vào một container.
 */
@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

	/** Cùng tag với docker-compose (spec 0002): collation ICU có phiên bản. */
	static final String POSTGRES_IMAGE = "postgres:18.7";

	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(POSTGRES_IMAGE);

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		return POSTGRES;
	}

}
