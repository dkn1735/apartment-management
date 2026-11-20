package vn.edu.hust.chungcu.canho.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Một tòa nhà trong danh mục (API-UC01-01, spec 0006 AC-7).
 *
 * @param soCanHo số căn hộ thuộc các tầng của tòa, tính lúc đọc
 */
public record ToaNhaResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maToaNha,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String tenToaNha,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int soTang,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String diaChiKhuVuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) long soCanHo) {
}
