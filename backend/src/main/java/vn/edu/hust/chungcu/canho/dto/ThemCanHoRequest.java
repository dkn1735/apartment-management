package vn.edu.hust.chungcu.canho.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import tools.jackson.databind.JsonNode;

/**
 * Body của {@code POST /api/v1/apartments} (spec 0012, AC-7, AC-8). Mọi trường đọc thành
 * {@link JsonNode} để giá trị sai kiểu ra lỗi trường thay vì {@code MALFORMED_REQUEST}.
 */
public record ThemCanHoRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = String.class,
				example = "A-01") JsonNode maTang,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = Integer.class, minimum = "1",
				maximum = "99", example = "5") JsonNode soCan,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = BigDecimal.class, minimum = "0",
				exclusiveMinimum = true, maximum = "1000", example = "70.5") JsonNode dienTich,
		@Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, type = "string", allowableValues = {
				"Trống", "Đang sử dụng", "Đang sửa chữa", "Đã bàn giao" },
				defaultValue = "Trống") JsonNode trangThaiSuDung){
}
