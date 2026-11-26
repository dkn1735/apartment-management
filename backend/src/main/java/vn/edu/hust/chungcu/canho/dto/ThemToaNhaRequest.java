package vn.edu.hust.chungcu.canho.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Body của {@code POST /api/v1/buildings} (spec 0011, AC-1, AC-2). Chuẩn hóa và kiểm ở
 * service; {@code soTang} đọc thành {@link JsonNode} để giá trị sai kiểu ra lỗi trường
 * thay vì {@code MALFORMED_REQUEST}.
 */
public record ThemToaNhaRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 10, example = "B") String maToaNha,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100, example = "Tòa B") String tenToaNha,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Integer.class, minimum = "1",
				maximum = "99", example = "5") JsonNode soTang,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255,
				example = "Khu đô thị mẫu, Hà Nội") String diaChiKhuVuc) {
}
