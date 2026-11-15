package vn.edu.hust.chungcu.common.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

	public record Security(@NotBlank(message = "Thiếu JWT_SECRET (khóa ký access token)") String jwtSecret) {
	}

}
