package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Body của {@code PUT /api/v1/accounts/{maNguoiDung}/status} (spec 0008, AC-10).
 * {@code lyDo} bắt buộc khi {@code trangThai} khác {@code Hoạt động}.
 */
public record DoiTrangThaiRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
		"Hoạt động", "Tạm khóa", "Ngừng hoạt động" }) String trangThai,
		@Schema(maxLength = 500, description = "Bắt buộc khi khóa hoặc ngừng hoạt động") String lyDo){
}
