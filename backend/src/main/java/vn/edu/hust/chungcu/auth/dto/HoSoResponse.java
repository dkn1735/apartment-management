package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * {@code data} của {@code GET /api/v1/auth/profile}: hồ sơ của chính người đang đăng
 * nhập. Không bao giờ có {@code matKhauHash} hay {@code soCccd}.
 */
public record HoSoResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maNguoiDung,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String soDienThoai,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String email,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "Hoạt động") String trangThaiTaiKhoan,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Đang dùng mật khẩu tạm, phải đổi trước khi dùng chức năng khác (spec 0008)") boolean phaiDoiMatKhau,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<VaiTro> vaiTro,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) PhamVi phamVi,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) ChiTietVaiTro chiTietVaiTro) {
}
