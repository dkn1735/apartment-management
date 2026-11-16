package vn.edu.hust.chungcu.auth.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.edu.hust.chungcu.common.config.AppProperties;

/**
 * Băm mật khẩu bằng bcrypt (spec 0004, mục Băm mật khẩu). Chỉ nhận id {@code bcrypt}:
 * hash {@code {noop}}, {@code {MD4}} hay id lạ lọt vào DB bị coi là sai mật khẩu. Không
 * dùng {@code PasswordEncoderFactories} vì nó nhận mọi id.
 */
@Configuration(proxyBeanMethods = false)
public class PasswordEncoderConfig {

	static final String BCRYPT = "bcrypt";

	@Bean
	PasswordEncoder passwordEncoder(AppProperties properties) {
		DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder(BCRYPT,
				Map.of(BCRYPT, new BCryptPasswordEncoder(properties.security().bcryptCost())));
		encoder.setDefaultPasswordEncoderForMatches(new RejectAllPasswordEncoder());
		return encoder;
	}

	/** Cho mọi hash không có id {@code bcrypt}: so khớp luôn sai, không bao giờ băm. */
	static final class RejectAllPasswordEncoder implements PasswordEncoder {

		@Override
		public String encode(CharSequence rawPassword) {
			throw new UnsupportedOperationException("Chỉ dùng để từ chối hash không phải bcrypt");
		}

		@Override
		public boolean matches(CharSequence rawPassword, String encodedPassword) {
			return false;
		}

	}

}
