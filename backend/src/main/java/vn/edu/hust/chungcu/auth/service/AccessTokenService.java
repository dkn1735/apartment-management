package vn.edu.hust.chungcu.auth.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.common.config.AppProperties;
import vn.edu.hust.chungcu.common.security.JwtConfig;

/** Ký access token JWT HS256 với claim cố định của spec 0004 (AC-7). */
@Service
public class AccessTokenService {

	private final JwtEncoder encoder;

	private final Duration ttl;

	public AccessTokenService(JwtEncoder encoder, AppProperties properties) {
		this.encoder = encoder;
		this.ttl = properties.security().accessTokenTtl();
	}

	/**
	 * @param phaiDoiMatKhau giá trị cột {@code phai_doi_mat_khau} đọc từ DB lúc cấp (spec
	 * 0008, AC-13)
	 */
	public AccessToken cap(String maNguoiDung, UUID maHoPhien, List<VaiTro> vaiTro, boolean phaiDoiMatKhau,
			Instant iat) {
		Instant exp = iat.plus(this.ttl);
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(JwtConfig.ISSUER)
			.subject(maNguoiDung)
			.id(UUID.randomUUID().toString())
			.issuedAt(iat)
			.expiresAt(exp)
			.claim(JwtConfig.CLAIM_SID, maHoPhien.toString())
			.claim(JwtConfig.CLAIM_VAI_TRO, vaiTro.stream().map(Enum::name).toList())
			.claim(JwtConfig.CLAIM_PHAI_DOI_MAT_KHAU, phaiDoiMatKhau)
			.build();
		// Phải ghi rõ header HS256: mặc định của NimbusJwtEncoder là RS256
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = this.encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new AccessToken(token, exp);
	}

	public record AccessToken(String value, Instant hetHanLuc) {

		@Override
		public String toString() {
			return "AccessToken[hetHanLuc=" + this.hetHanLuc + "]";
		}

	}

}
