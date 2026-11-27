package vn.edu.hust.chungcu.phuongtien.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import tools.jackson.databind.JsonNode;

/**
 * Body của {@code POST /api/v1/vehicles} (spec 0016, AC-5, AC-6). Mọi trường đọc thành
 * {@link JsonNode} để giá trị sai kiểu ra lỗi trường.
 */
public record DangKyPhuongTienRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", example = "A-0101") JsonNode maCanHo,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string",
				example = "CD00000001") JsonNode maCuDanSoHuu,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", allowableValues = {
				"Ô tô", "Xe máy", "Xe máy điện", "Xe đạp" }) JsonNode loaiPhuongTien,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", nullable = true,
				example = "30A-123.45", description = "Bắt buộc trừ khi loại là Xe đạp") JsonNode bienSo,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", nullable = true,
				maxLength = 100) JsonNode nhanHieuMauSac){
}
