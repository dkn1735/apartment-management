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
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.edu.hust.chungcu.common.error.ErrorResponse;
import vn.edu.hust.chungcu.common.security.CsrfHeaderFilter;

/**
 * Mô tả OpenAPI dùng chung: gắn schema {@code ErrorResponse} vào components và làm
 * response {@code default} của mọi endpoint, để kiểu sinh ra ở frontend có lỗi đúng dạng.
 * Kiểu thành công đã bọc ({@code ApiResponse<T>}) springdoc tự đọc từ kiểu trả về
 * {@code ResponseEntity<ApiResponse<T>>} của controller.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

	private static final String ERROR_SCHEMA = "ErrorResponse";

	private static final String CSRF_SCHEME = "csrfHeader";

	@Bean
	OpenAPI chungCuOpenApi() {
		// Server tương đối: frontend gọi qua cùng tên miền, và file xuất ra không phụ
		// thuộc máy
		// Header chống CSRF (spec 0004): bấm "Authorize" trong Swagger, nhập
		// XMLHttpRequest một
		// lần. Scheme không đổi kiểu tham số mà pnpm gen:api sinh ra.
		SecurityScheme csrf = new SecurityScheme().type(SecurityScheme.Type.APIKEY)
			.in(SecurityScheme.In.HEADER)
			.name(CsrfHeaderFilter.HEADER)
			.description("Nhập XMLHttpRequest. Bắt buộc với POST, PUT, PATCH, DELETE.");
		return new OpenAPI().info(new Info().title("Hệ thống Quản lý Chung cư API").version("v1"))
			.servers(List.of(new Server().url("/")))
			.components(new Components().addSecuritySchemes(CSRF_SCHEME, csrf))
			.addSecurityItem(new SecurityRequirement().addList(CSRF_SCHEME));
	}

	/**
	 * Thuộc tính trỏ tới schema khác mà khai {@code nullable = true}: springdoc ghi
	 * {@code $ref} cạnh {@code type: null}, và trình sinh kiểu bỏ mất {@code null}. Đổi
	 * thành {@code oneOf: [$ref, null]} để frontend nhận đúng {@code T | null} (spec
	 * 0013, DTO).
	 */
	@Bean
	OpenApiCustomizer nullableRefCustomizer() {
		return openApi -> {
			if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
				return;
			}
			for (Schema<?> schema : openApi.getComponents().getSchemas().values()) {
				Map<String, Schema> properties = schema.getProperties();
				if (properties == null) {
					continue;
				}
				properties.replaceAll((name, property) -> {
					boolean nullableRef = property.get$ref() != null && property.getTypes() != null
							&& property.getTypes().contains("null");
					if (!nullableRef) {
						return property;
					}
					return new Schema<>().description(property.getDescription())
						.oneOf(List.of(new Schema<>().$ref(property.get$ref()), new Schema<>().types(Set.of("null"))));
				});
			}
		};
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
							.content(new Content()
								.addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, new MediaType()
									.schema(new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA)))))));
		};
	}

}
