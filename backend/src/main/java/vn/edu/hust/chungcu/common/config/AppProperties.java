package vn.edu.hust.chungcu.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
		@Valid @NotNull Security security,
		@NotBlank(message = "Thiếu APP_PUBLIC_URL (URL gốc của frontend, dùng để dựng link trong email)") @Pattern(
				regexp = "^https?://.+",
				message = "APP_PUBLIC_URL phải bắt đầu bằng http:// hoặc https://") String publicUrl,
		@Valid @NotNull(message = "Thiếu MAIL_FROM (địa chỉ người gửi email)") Mail mail) {

	/**
	 * Bỏ dấu {@code /} ở cuối {@code publicUrl}, để link dựng bằng cách nối chuỗi không
	 * có hai dấu {@code /} (spec 0010, AC-14).
	 */
	public AppProperties {
		if (publicUrl != null) {
			publicUrl = publicUrl.strip();
			while (publicUrl.endsWith("/")) {
				publicUrl = publicUrl.substring(0, publicUrl.length() - 1);
			}
		}
	}

	/**
	 * Cấu hình gửi email (spec 0010).
	 *
	 * @param from địa chỉ người gửi, có thể kèm tên hiển thị; với Gmail phải trùng
	 * {@code MAIL_USERNAME}
	 */
	public record Mail(@NotBlank(message = "Thiếu MAIL_FROM (địa chỉ người gửi email)") String from) {

	}

	/**
	 * Cấu hình xác thực (spec 0004, mục Configuration required).
	 *
	 * @param jwtSecret khóa ký HS256 của access token, ít nhất 32 byte UTF-8
	 * @param accessTokenTtl thời gian sống của access token
	 * @param refreshTokenTtl thời gian sống của refresh token và cookie {@code phien}
	 * @param bcryptCost cost của bcrypt khi băm mật khẩu mới
	 * @param cookieSecure có đặt cờ {@code Secure} cho cookie xác thực không
	 * @param sessionMaxLifetime tuổi tối đa của một họ phiên kể từ lần đăng nhập đầu
	 * (spec 0005)
	 * @param refreshReuseGrace khoảng khoan dung cho các request làm mới chạy đua và cho
	 * response làm mới bị mất; lớn hơn 0 và không quá 60 giây
	 * @param sessionRetention giữ dòng đã hết hạn hoặc thu hồi bao lâu trước khi xóa;
	 * phải không nhỏ hơn {@code sessionMaxLifetime}
	 * @param sessionCleanupCron lịch job dọn dẹp phiên, theo {@code app.timezone}
	 * @param passwordResetTtl thời gian sống của link đặt lại mật khẩu qua email (spec
	 * 0010)
	 */
	public record Security(@NotBlank(message = "Thiếu JWT_SECRET (khóa ký access token)") String jwtSecret,
			@NotNull(message = "Thiếu app.security.access-token-ttl") Duration accessTokenTtl,
			@NotNull(message = "Thiếu app.security.refresh-token-ttl") Duration refreshTokenTtl,
			@Min(value = 4, message = "app.security.bcrypt-cost phải từ 4 tới 31") @Max(value = 31,
					message = "app.security.bcrypt-cost phải từ 4 tới 31") int bcryptCost,
			boolean cookieSecure,
			@NotNull(message = "Thiếu app.security.session-max-lifetime") Duration sessionMaxLifetime,
			@NotNull(message = "Thiếu app.security.refresh-reuse-grace") Duration refreshReuseGrace,
			@NotNull(message = "Thiếu app.security.session-retention") Duration sessionRetention,
			@NotBlank(message = "Thiếu app.security.session-cleanup-cron") String sessionCleanupCron,
			@NotNull(message = "Thiếu app.security.password-reset-ttl") Duration passwordResetTtl) {

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

		private static final Duration KHOAN_DUNG_TOI_DA = Duration.ofSeconds(60);

		/**
		 * Lỗi cấu hình không được âm thầm nới rộng khoảng mà token bị lộ vẫn dùng được
		 * (spec 0005, Configuration required).
		 */
		@AssertTrue(message = "app.security.refresh-reuse-grace phải lớn hơn 0 và không quá PT60S")
		public boolean isRefreshReuseGraceHopLe() {
			return this.refreshReuseGrace == null || (this.refreshReuseGrace.isPositive()
					&& this.refreshReuseGrace.compareTo(KHOAN_DUNG_TOI_DA) <= 0);
		}

		/**
		 * Dòng {@code Đã xoay vòng} phải sống ít nhất bằng tuổi tối đa của họ phiên, để
		 * dùng lại token cũ luôn bị phát hiện chừng nào họ còn sống (spec 0005).
		 */
		@AssertTrue(message = "app.security.session-retention phải không nhỏ hơn app.security.session-max-lifetime")
		public boolean isSessionRetentionDuDai() {
			return this.sessionRetention == null || this.sessionMaxLifetime == null
					|| this.sessionRetention.compareTo(this.sessionMaxLifetime) >= 0;
		}

	}

}
