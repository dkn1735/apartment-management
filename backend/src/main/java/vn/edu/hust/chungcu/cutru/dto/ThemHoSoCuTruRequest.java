package vn.edu.hust.chungcu.cutru.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Body của {@code POST /api/v1/residency-records} (spec 0014, AC-4, AC-5). Mọi trường đọc
 * thành {@link JsonNode} để giá trị sai kiểu ra lỗi trường thay vì
 * {@code MALFORMED_REQUEST}.
 */
public record ThemHoSoCuTruRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", example = "A-0102") JsonNode maCanHo,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", example = "CD00000001") JsonNode maCuDan,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", allowableValues = {
				"Chủ căn hộ", "Khách thuê", "Thành viên gia đình" }) JsonNode vaiTroCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string",
				allowableValues = { "Thường trú", "Tạm trú" }) JsonNode tinhTrangCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date",
				example = "2026-11-16") JsonNode ngayBatDau,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", format = "date", nullable = true,
				description = "Bắt buộc khi tình trạng là Tạm trú") JsonNode ngayKetThuc){
}
