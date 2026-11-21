package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Kết quả đặt lại mật khẩu (spec 0008, AC-12). {@code matKhauTam} chỉ có ở response này.
 */
public record DatLaiMatKhauResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "XXXXXXXXXXXX",
		description = "Mật khẩu tạm, chỉ hiện một lần") String matKhauTam) {

	/** Không bao giờ in mật khẩu tạm. */
	@Override
	public String toString() {
		return "DatLaiMatKhauResponse[matKhauTam=***]";
	}

}
