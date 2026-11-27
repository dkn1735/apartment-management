package vn.edu.hust.chungcu.cutru.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Body của {@code PUT /api/v1/residency-records/{recordId}} (spec 0014, AC-7, AC-8). Căn
 * hộ và cư dân của hồ sơ không đổi qua API này.
 */
public record CapNhatHoSoCuTruRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", allowableValues = {
				"Chủ căn hộ", "Khách thuê", "Thành viên gia đình" }) JsonNode vaiTroCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string",
				allowableValues = { "Thường trú", "Tạm trú" }) JsonNode tinhTrangCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date",
				example = "2026-11-16") JsonNode ngayBatDau,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", format = "date", nullable = true,
				description = "Bắt buộc khi Tạm trú hoặc khi kết thúc hồ sơ") JsonNode ngayKetThuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string",
				allowableValues = { "Hợp lệ", "Hết hiệu lực" }) JsonNode trangThaiHoSo){
}
