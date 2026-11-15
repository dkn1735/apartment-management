package vn.edu.hust.chungcu;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Xuất đặc tả OpenAPI ra {@code docs/api/openapi.json} để frontend sinh kiểu
 * ({@code pnpm gen:api}). Chạy bằng {@code mvnw.cmd verify}; commit file kèm thay đổi
 * API.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "app.security.jwt-secret=test-only-secret-not-for-real-use")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class OpenApiExportTest {

	private static final Path OUTPUT = Path.of("..", "docs", "api", "openapi.json");

	@Autowired
	MockMvc mockMvc;

	@Test
	void exportOpenApi() throws Exception {
		String body = mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);

		JsonMapper mapper = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();
		JsonNode tree = mapper.readTree(body);
		Files.createDirectories(OUTPUT.getParent());
		Files.writeString(OUTPUT, mapper.writeValueAsString(tree) + "\n", StandardCharsets.UTF_8);
	}

}
