package vn.edu.hust.chungcu.cutru.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Nhân khẩu, lịch sử và đề nghị cư trú của một căn (spec 0014 AC-1, spec 0015 AC-8). Bốn
 * mảng không bao giờ {@code null}.
 */
public record HoSoCuTruCanHoResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<HoSoCuTruResponse> dangCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<HoSoCuTruResponse> sapBatDau,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<HoSoCuTruResponse> lichSuCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<HoSoCuTruResponse> deNghiCuTru) {

	public HoSoCuTruCanHoResponse {
		dangCuTru = List.copyOf(dangCuTru);
		sapBatDau = List.copyOf(sapBatDau);
		lichSuCuTru = List.copyOf(lichSuCuTru);
		deNghiCuTru = List.copyOf(deNghiCuTru);
	}

}
