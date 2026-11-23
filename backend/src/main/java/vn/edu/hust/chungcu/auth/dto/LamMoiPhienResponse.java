package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * {@code data} của làm mới phiên thành công (spec 0005, AC-1, AC-5). Token nằm trong
 * cookie, không bao giờ ở body.
 *
 * @param lamMoi {@code true} khi vừa xoay ra refresh token mới; {@code false} khi token
 * gửi lên vừa được xoay trong khoảng khoan dung và token đang sống được trả lại. Cả hai
 * đều đặt ba cookie.
 * @param accessTokenHetHanLuc lúc access token mới hết hạn, không bao giờ {@code null}
 */
public record LamMoiPhienResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean lamMoi,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maNguoiDung,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<VaiTro> vaiTro,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED,
				description = "Đang dùng mật khẩu tạm, phải đổi trước khi dùng chức năng khác (spec 0008)") boolean phaiDoiMatKhau,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) Instant accessTokenHetHanLuc) {
}
