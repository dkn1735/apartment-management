package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/**
 * Body của {@code POST /api/v1/auth/password-reset/request} (spec 0010, AC-2). Email được
 * chuẩn hóa ({@code strip()}, viết thường) ngay khi dựng, trước bean validation, nên quy
 * tắc áp lên giá trị đã chuẩn hóa. Quy tắc và câu lỗi giống ô email khi tạo tài khoản.
 */
public record YeuCauDatLaiRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 150,
		example = "an.nguyen@example.com") @NotBlank(message = "Vui lòng nhập email") @Size(max = 150,
				message = "Email không hợp lệ") @Pattern(regexp = EMAIL_REGEX,
						message = "Email không hợp lệ") String email) {

	/** Định dạng email sau chuẩn hóa, dùng chung với màn tạo và sửa tài khoản. */
	public static final String EMAIL_REGEX = "^[a-z0-9._%+-]+@[a-z0-9-]+(\\.[a-z0-9-]+)*\\.[a-z]{2,}$";

	public YeuCauDatLaiRequest {
		if (email != null) {
			email = email.strip().toLowerCase(Locale.ROOT);
		}
	}

	/** Không in email ra log. */
	@Override
	public String toString() {
		return "YeuCauDatLaiRequest[***]";
	}

}
