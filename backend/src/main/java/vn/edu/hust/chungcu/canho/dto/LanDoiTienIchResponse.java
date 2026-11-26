package vn.edu.hust.chungcu.canho.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Lần gần nhất cờ tạm dừng tiện ích của căn đổi giá trị (spec 0013, AC-8). Chiều của lần
 * đổi suy ra từ cờ hiện tại của căn. Người đổi là {@code null} khi người xem là cư dân
 * (AC-9).
 */
public record LanDoiTienIchResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Nợ phí quý 3") String lyDo,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant doiLuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "ND00000123") String maNguoiDoi,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "Phạm Kế Toán") String hoTenNguoiDoi) {

	/** Bản có họ tên người đổi. */
	public LanDoiTienIchResponse voiHoTen(String hoTen) {
		return new LanDoiTienIchResponse(this.lyDo, this.doiLuc, this.maNguoiDoi, hoTen);
	}

	/** Bản không có người đổi, cho cư dân (AC-9). */
	public LanDoiTienIchResponse anNguoiDoi() {
		return new LanDoiTienIchResponse(this.lyDo, this.doiLuc, null, null);
	}

}
