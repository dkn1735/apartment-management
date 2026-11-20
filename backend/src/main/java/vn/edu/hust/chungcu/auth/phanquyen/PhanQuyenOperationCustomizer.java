package vn.edu.hust.chungcu.auth.phanquyen;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import vn.edu.hust.chungcu.auth.dto.VaiTro;

/**
 * Ghi quyền của mỗi operation vào OpenAPI từ annotation (spec 0006, AC-5), để Swagger và
 * kiểu sinh ở frontend luôn khớp code.
 */
@Component
public class PhanQuyenOperationCustomizer implements OperationCustomizer {

	static final String CONG_KHAI = "Công khai, không cần đăng nhập";

	static final String DA_DANG_NHAP = "Vai trò được phép: mọi người đã đăng nhập";

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		ChoPhep choPhep = handlerMethod.getMethodAnnotation(ChoPhep.class);
		if (choPhep != null) {
			String vaiTro = Arrays.stream(choPhep.value())
				.sorted()
				.distinct()
				.map(VaiTro::name)
				.collect(Collectors.joining(", "));
			themDong(operation, "Vai trò được phép: " + vaiTro);
			them403(operation);
		}
		else if (handlerMethod.hasMethodAnnotation(DaDangNhap.class)) {
			themDong(operation, DA_DANG_NHAP);
		}
		else if (handlerMethod.hasMethodAnnotation(CongKhai.class)) {
			themDong(operation, CONG_KHAI);
		}
		return operation;
	}

	private static void themDong(Operation operation, String dong) {
		String moTa = operation.getDescription();
		operation.setDescription((moTa == null || moTa.isBlank()) ? dong : moTa + "\n\n" + dong);
	}

	private static void them403(Operation operation) {
		ApiResponses responses = operation.getResponses();
		if (responses == null) {
			responses = new ApiResponses();
			operation.setResponses(responses);
		}
		if (responses.containsKey("403")) {
			return;
		}
		responses.addApiResponse("403",
				new ApiResponse().description("Không đủ quyền (FORBIDDEN)")
					.content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
							new MediaType().schema(new Schema<>().$ref("#/components/schemas/ErrorResponse")))));
	}

}
