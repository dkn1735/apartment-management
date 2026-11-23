package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Body của {@code POST /api/v1/accounts} (spec 0008, AC-1). Chuẩn hóa và kiểm ở service
 * để lỗi có đúng tên trường và thứ tự của AC-4.
 */
public record TaoTaiKhoanRequest(
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 100, example = "Nguyễn Văn An") String hoTen,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "0912345678") String soDienThoai,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150,
				example = "an.nguyen@example.com") String email,
		@ArraySchema(minItems = 1, maxItems = 7, schema = @Schema(implementation = VaiTroRequest.class)) @Schema(
				requiredMode = Schema.RequiredMode.REQUIRED) List<VaiTroRequest> vaiTro) {

	/** Không in số điện thoại, email, CCCD ra log. */
	@Override
	public String toString() {
		return "TaoTaiKhoanRequest[***]";
	}

}
