package vn.edu.hust.chungcu.canho.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import tools.jackson.databind.JsonNode;

/**
 * Body của {@code PUT /api/v1/apartments/{apartmentId}} (spec 0012, AC-11, AC-12). Mã
 * căn, tầng và cờ tạm dừng tiện ích không đổi qua API này.
 */
public record SuaCanHoRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, implementation = BigDecimal.class, minimum = "0",
				exclusiveMinimum = true, maximum = "1000", example = "72") JsonNode dienTich,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, type = "string", allowableValues = {
				"Trống", "Đang sử dụng", "Đang sửa chữa", "Đã bàn giao" }) JsonNode trangThaiSuDung){
}
