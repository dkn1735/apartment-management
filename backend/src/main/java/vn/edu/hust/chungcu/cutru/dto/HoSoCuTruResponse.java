package vn.edu.hust.chungcu.cutru.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Một hồ sơ cư trú hoặc đề nghị cư trú (spec 0014 AC-2, spec 0015 AC-8). Khi người xem
 * chỉ có quyền cư dân của căn, server đặt {@code null} cho {@code maNguoiDung},
 * {@code soDienThoai}, {@code email}, {@code soCccd}, {@code maNvbqlXacMinh} và
 * {@code hoTenNguoiXacMinh}; ngoài mảng đề nghị thì ẩn cả bốn trường người đề nghị.
 */
public record HoSoCuTruResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "HS00000001") String maHoSo,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "A-0101") String maCanHo,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "CD00000001") String maCuDan,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "ND00000007") String maNguoiDung,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "Vũ Cư Dân") String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "0900000007") String soDienThoai,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "cudan@chungcu.test") String email,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "001026000007") String soCccd,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
				"Chủ căn hộ", "Khách thuê", "Thành viên gia đình" }) String vaiTroCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				allowableValues = { "Thường trú", "Tạm trú" }) String tinhTrangCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate ngayBatDau,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) LocalDate ngayKetThuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				allowableValues = { "Chờ xác minh", "Hợp lệ", "Hết hiệu lực", "Từ chối",
						"Đã hủy" }) String trangThaiHoSo,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "BQL00000001") String maNvbqlXacMinh,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "Trần Ban Quản Lý") String hoTenNguoiXacMinh,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant taoLuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant capNhatLuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "CD00000002") String maCuDanDeNghi,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "Trần Thị Mai") String hoTenNguoiDeNghi,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String ghiChuDeNghi,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String lyDoTuChoi,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Người xem là người gửi đề nghị đang chờ xác minh") boolean coTheHuy){
}
