package vn.edu.hust.chungcu.phuongtien.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** Xe đang gửi, đơn chờ và lịch sử của một căn (spec 0016, AC-2). */
public record PhuongTienCanHoResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PhuongTienResponse> dangGui,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<PhuongTienResponse> donVaLichSu) {
}
