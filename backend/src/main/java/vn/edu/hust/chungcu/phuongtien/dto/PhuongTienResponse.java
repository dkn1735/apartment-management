package vn.edu.hust.chungcu.phuongtien.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Một phương tiện của căn hộ (spec 0016, AC-2, AC-3). Không bao giờ có số CCCD, số điện
 * thoại hay email. Góc nhìn cư dân đặt {@code null} cho người cập nhật, và cho lý do của
 * dòng không liên quan tới người xem.
 */
public record PhuongTienResponse(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "PT00000001") String maPhuongTien,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "A-0101") String maCanHo,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {
				"Ô tô", "Xe máy", "Xe máy điện", "Xe đạp" }) String loaiPhuongTien,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true, example = "30A12345") String bienSo,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "Toyota trắng") String nhanHieuMauSac,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				allowableValues = { "Chờ duyệt", "Đang hoạt động", "Tạm dừng", "Từ chối",
						"Đã hủy" }) String trangThaiDangKy,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "CD00000001") String maCuDanSoHuu,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Vũ Cư Dân") String hoTenChuXe,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Chủ xe đang cư trú hợp lệ hôm nay tại căn của xe") boolean chuXeConCuTru,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "CD00000002") String maCuDanDangKy,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String hoTenNguoiDangKy,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String lyDoTrangThai,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true,
				example = "BQL00000001") String maNvbqlCapNhat,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, nullable = true) String hoTenNguoiCapNhat,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant taoLuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant capNhatLuc,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Người xem là người gửi đơn đang chờ duyệt") boolean coTheRutDon){
}
