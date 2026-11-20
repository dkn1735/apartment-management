package vn.edu.hust.chungcu.auth.phanquyen;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.edu.hust.chungcu.TestcontainersConfiguration;

/** Quyền của mỗi operation hiện trong OpenAPI (spec 0006, AC-5). */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({ "dev", "test" })
class PhanQuyenOpenApiTest {

	@Autowired
	MockMvc mockMvc;

	@Test
	void moTaVaResponse403SinhTuAnnotation() throws Exception {
		this.mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.paths['/api/v1/buildings'].get.description")
				.value(endsWith("\n\nVai trò được phép: BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN")))
			.andExpect(jsonPath("$.paths['/api/v1/buildings'].get.responses['403'].description")
				.value("Không đủ quyền (FORBIDDEN)"))
			.andExpect(jsonPath(
					"$.paths['/api/v1/buildings'].get.responses['403'].content['application/json'].schema['$ref']")
				.value("#/components/schemas/ErrorResponse"))
			.andExpect(jsonPath("$.paths['/api/v1/auth/profile'].get.description")
				.value("Vai trò được phép: mọi người đã đăng nhập"))
			.andExpect(jsonPath("$.paths['/api/v1/auth/profile'].get.responses['403']").doesNotExist())
			.andExpect(jsonPath("$.paths['/api/v1/auth/login'].post.description")
				.value(endsWith("\n\nCông khai, không cần đăng nhập")));
	}

}
