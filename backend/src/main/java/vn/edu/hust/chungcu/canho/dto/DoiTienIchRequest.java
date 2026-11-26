package vn.edu.hust.chungcu.canho.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Body của {@code PATCH /api/v1/apartments/{apartmentId}/facility-suspension} (spec 0013,
 * AC-1, AC-3). Đọc thành {@link JsonNode} để service tự kiểm kiểu; trường khác bị bỏ qua.
 */
public record DoiTienIchRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Boolean.class,
				description = "true là tạm dừng, false là mở khóa") JsonNode biTamDungTienIch,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", maxLength = 500,
				example = "Nợ phí quý 3") JsonNode lyDo) {
}
