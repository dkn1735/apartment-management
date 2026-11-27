package vn.edu.hust.chungcu.cutru.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Body của {@code POST /api/v1/residency-records/resident-request} (spec 0015, AC-1,
 * AC-2). Mọi trường đọc thành {@link JsonNode} để giá trị sai kiểu ra lỗi trường.
 */
public record GuiDeNghiCuTruRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", example = "A-0101") JsonNode maCanHo,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "boolean",
				description = "true khi đăng ký cho chính người gửi") JsonNode laChinhToi,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", example = "001026000010",
				description = "Bắt buộc khi laChinhToi = false") JsonNode soCccd,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", example = "0900000010",
				description = "Bắt buộc khi laChinhToi = false") JsonNode soDienThoai,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", allowableValues = {
				"Thành viên gia đình", "Khách thuê" }) JsonNode vaiTroCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string",
				allowableValues = { "Thường trú", "Tạm trú" }) JsonNode tinhTrangCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date",
				example = "2026-12-01") JsonNode ngayBatDau,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", format = "date", nullable = true,
				description = "Bắt buộc khi tình trạng là Tạm trú") JsonNode ngayKetThuc,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", nullable = true,
				maxLength = 500) JsonNode ghiChuDeNghi){
}
