package vn.edu.hust.chungcu.common.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import vn.edu.hust.chungcu.common.config.AppProperties;

/**
 * Ký và kiểm access token JWT HS256 bằng {@code JWT_SECRET} (spec 0004, mục Token). Claim
 * {@value #CLAIM_VAI_TRO} thành authority {@code ROLE_<mã>}, tên principal là
 * {@code sub}.
 */
@Configuration(proxyBeanMethods = false)
public class JwtConfig {

	public static final String ISSUER = "chungcu";

	public static final String CLAIM_VAI_TRO = "vaiTro";

	public static final String CLAIM_SID = "sid";

	private static final Duration CLOCK_SKEW = Duration.ofSeconds(30);

	@Bean
	JwtEncoder jwtEncoder(AppProperties properties) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(signingKey(properties)));
	}

	@Bean
	JwtDecoder jwtDecoder(AppProperties properties, Clock clock) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(signingKey(properties))
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		JwtTimestampValidator timestamps = new JwtTimestampValidator(CLOCK_SKEW);
		timestamps.setClock(clock);
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(ISSUER)));
		return decoder;
	}

	@Bean
	JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName(CLAIM_VAI_TRO);
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		converter.setPrincipalClaimName("sub");
		return converter;
	}

	private static SecretKey signingKey(AppProperties properties) {
		byte[] secret = properties.security().jwtSecret().getBytes(StandardCharsets.UTF_8);
		return new SecretKeySpec(secret, "HmacSHA256");
	}

}
