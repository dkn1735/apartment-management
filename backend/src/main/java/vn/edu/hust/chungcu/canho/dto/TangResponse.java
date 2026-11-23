package vn.edu.hust.chungcu.canho.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Một tầng của tòa nhà (spec 0011, AC-8).
 *
 * @param soCanHo số căn hộ của tầng, tính lúc đọc
 */
public record TangResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maTang,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) int soTang,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) long soCanHo) {
}
