package vn.edu.hust.chungcu.nhatky.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

/**
 * Một dòng nhật ký trong danh sách (API-AUTH-05, spec 0007 AC-11).
 *
 * @param maNguoiDung người thực hiện; {@code null} khi không xác định được người
 * @param hoTen họ tên hiện tại của người thực hiện, không phải tên lúc thao tác
 * @param tenThaoTac nhãn của {@code loaiThaoTac}; mã không còn trong danh mục thì là
 * chính mã
 * @param chiTietThayDoi JSON object hoặc {@code null}
 */
public record NhatKyResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maLog,
		@Schema(nullable = true) String maNguoiDung, @Schema(nullable = true) String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String loaiThaoTac,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String tenThaoTac,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String doiTuongTacDong, @Schema(types = {
				"object", "null" },
				additionalProperties = Schema.AdditionalPropertiesValue.TRUE) Map<String, Object> chiTietThayDoi,
		@Schema(nullable = true) String diaChiIp, @Schema(nullable = true) String requestId,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant thoiGianGhi){
}
