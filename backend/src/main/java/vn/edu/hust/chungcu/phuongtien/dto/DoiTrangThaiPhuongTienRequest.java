package vn.edu.hust.chungcu.phuongtien.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/** Body của {@code PUT /api/v1/vehicles/{vehicleId}/status} (spec 0016, AC-9). */
public record DoiTrangThaiPhuongTienRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", allowableValues = {
				"Đang hoạt động", "Tạm dừng", "Từ chối", "Đã hủy" }) JsonNode trangThaiDangKy,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", nullable = true, maxLength = 500,
				description = "Bắt buộc khi tạm dừng, từ chối hoặc hủy") JsonNode lyDo){
}
