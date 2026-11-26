package vn.edu.hust.chungcu.canho.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

/**
 * Một căn hộ kèm tầng và tòa của nó (API-UC01-05 đến 09, spec 0012, spec 0013). Không có
 * dữ liệu cá nhân.
 */
public record CanHoResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "A-0105") String maCanHo,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "A-01") String maTang,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "1") int soTang,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "A") String maToaNha,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Tòa A") String tenToaNha,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "75.50") BigDecimal dienTich,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
				"Trống", "Đang sử dụng", "Đang sửa chữa", "Đã bàn giao" }) String trangThaiSuDung,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean biTamDungTienIch,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				description = "Lần đổi tiện ích gần nhất, null khi căn chưa từng đổi") LanDoiTienIchResponse lanDoiTienIch){

	/** Bản có lần đổi tiện ích thay bằng {@code lanDoi}. */
	public CanHoResponse voiLanDoi(LanDoiTienIchResponse lanDoi) {
		return new CanHoResponse(this.maCanHo, this.maTang, this.soTang, this.maToaNha, this.tenToaNha, this.dienTich,
				this.trangThaiSuDung, this.biTamDungTienIch, lanDoi);
	}

}
