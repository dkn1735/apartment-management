package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * {@code data} của đăng nhập thành công. Token nằm trong cookie, không bao giờ ở body.
 */
public record DangNhapResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maNguoiDung,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<VaiTro> vaiTro,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Đang dùng mật khẩu tạm, phải đổi trước khi dùng chức năng khác (spec 0008)") boolean phaiDoiMatKhau,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant accessTokenHetHanLuc) {
}
