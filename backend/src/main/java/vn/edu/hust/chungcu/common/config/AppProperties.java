package vn.edu.hust.chungcu.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cấu hình riêng của ứng dụng (tiền tố {@code app}). Thiếu biến bắt buộc thì backend dừng
 * ngay lúc khởi động với thông báo rõ ràng.
 */
@Validated
@ConfigurationProperties("app")
public record AppProperties(@NotNull(message = "Thiếu APP_TIMEZONE (múi giờ cho quy tắc theo ngày)") ZoneId timezone,
		@Valid @NotNull Security security) {

	/**
	 * Cấu hình xác thực (spec 0004, mục Configuration required).
	 *
	 * @param jwtSecret khóa ký HS256 của access token, ít nhất 32 byte UTF-8
	 * @param accessTokenTtl thời gian sống của access token
	 * @param refreshTokenTtl thời gian sống của refresh token và cookie {@code phien}
	 * @param bcryptCost cost của bcrypt khi băm mật khẩu mới
	 * @param cookieSecure có đặt cờ {@code Secure} cho cookie xác thực không
	 */
	public record Security(@NotBlank(message = "Thiếu JWT_SECRET (khóa ký access token)") String jwtSecret,
			@NotNull(message = "Thiếu app.security.access-token-ttl") Duration accessTokenTtl,
			@NotNull(message = "Thiếu app.security.refresh-token-ttl") Duration refreshTokenTtl,
			@Min(value = 4, message = "app.security.bcrypt-cost phải từ 4 tới 31") @Max(value = 31,
					message = "app.security.bcrypt-cost phải từ 4 tới 31") int bcryptCost,
			boolean cookieSecure) {

		/** Chuỗi mẫu trong {@code backend/.env.example}; backend không đọc file đó. */
		public static final String JWT_SECRET_MAU = "doi-thanh-chuoi-ngau-nhien-du-dai";

		private static final int JWT_SECRET_TOI_THIEU_BYTE = 32;

		@AssertTrue(message = "JWT_SECRET phải dài ít nhất 32 byte (gợi ý: openssl rand -base64 48)")
		public boolean isJwtSecretDuDai() {
			return this.jwtSecret == null || this.jwtSecret.isBlank()
					|| this.jwtSecret.getBytes(StandardCharsets.UTF_8).length >= JWT_SECRET_TOI_THIEU_BYTE;
		}

		@AssertTrue(message = "JWT_SECRET đang là chuỗi mẫu của backend/.env.example, hãy đổi thành chuỗi ngẫu nhiên")
		public boolean isJwtSecretKhacChuoiMau() {
			return !JWT_SECRET_MAU.equals(this.jwtSecret);
		}

	}

}
