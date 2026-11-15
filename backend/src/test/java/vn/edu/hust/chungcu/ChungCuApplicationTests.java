package vn.edu.hust.chungcu;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-not-for-real-use")
class ChungCuApplicationTests {

	@Test
	void contextLoads() {
	}

}
