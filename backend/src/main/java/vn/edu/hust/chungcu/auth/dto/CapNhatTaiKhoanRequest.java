package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body của {@code PUT /api/v1/accounts/{maNguoiDung}} (spec 0008, AC-7). Cả bốn trường
 * bắt buộc; chuẩn hóa và kiểm như khi tạo.
 */
public record CapNhatTaiKhoanRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100) String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String soDienThoai,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150) String email,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "0",
				description = "phienBan nhận ở chi tiết tài khoản") Long phienBan) {

	@Override
	public String toString() {
		return "CapNhatTaiKhoanRequest[phienBan=" + this.phienBan + ", ***]";
	}

}
