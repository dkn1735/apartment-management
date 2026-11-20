package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * {@code data} của làm mới phiên thành công (spec 0005, AC-1, AC-5). Token nằm trong
 * cookie, không bao giờ ở body.
 *
 * @param lamMoi {@code true} khi vừa xoay vòng và đặt cookie mới; {@code false} khi token
 * vừa được xoay bởi một request khác trong khoảng khoan dung (không có cookie mới)
 * @param accessTokenHetHanLuc lúc access token mới hết hạn; {@code null} khi
 * {@code lamMoi} là {@code false}
 */
public record LamMoiPhienResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean lamMoi,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String maNguoiDung,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<VaiTro> vaiTro,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, types = {
				"string", "null" }, format = "date-time") Instant accessTokenHetHanLuc){
}
