package vn.edu.hust.chungcu.common.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.hust.chungcu.common.error.ErrorResponse;

/**
 * Mô tả OpenAPI dùng chung: gắn schema {@code ErrorResponse} vào components và làm
 * response {@code default} của mọi endpoint, để kiểu sinh ra ở frontend có lỗi đúng dạng.
 * Kiểu thành công đã bọc ({@code ApiResponse<T>}) springdoc tự đọc từ kiểu trả về
 * {@code ResponseEntity<ApiResponse<T>>} của controller.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	private static final String ERROR_SCHEMA = "ErrorResponse";

	@Bean
	OpenAPI chungCuOpenApi() {
		// Server tương đối: frontend gọi qua cùng tên miền, và file xuất ra không phụ thuộc máy
		return new OpenAPI().info(new Info().title("Hệ thống Quản lý Chung cư API").version("v1"))
			.servers(List.of(new Server().url("/")));
	}

	@Bean
	OpenApiCustomizer errorResponseCustomizer() {
		return openApi -> {
			Components components = openApi.getComponents() != null ? openApi.getComponents() : new Components();
			openApi.setComponents(components);
			ResolvedSchema resolved = ModelConverters.getInstance().readAllAsResolvedSchema(ErrorResponse.class);
			for (Map.Entry<String, Schema> entry : resolved.referencedSchemas.entrySet()) {
				components.addSchemas(entry.getKey(), entry.getValue());
			}
			if (openApi.getPaths() == null) {
				return;
			}
			openApi.getPaths()
				.values()
				.forEach(path -> path.readOperations()
					.forEach(operation -> operation.getResponses()
						.addApiResponse("default", new ApiResponse().description("Lỗi theo vỏ bọc chung")
							.content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
									new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA)))))));
		};
	}

}
