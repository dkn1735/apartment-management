package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import vn.edu.hust.chungcu.auth.dto.ChiTietVaiTro;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTroNguoiDung;
import vn.edu.hust.chungcu.auth.repository.VaiTroQuery;
import vn.edu.hust.chungcu.canho.service.ToaNhaService;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.cutru.service.CuTruService;

/** {@link PhamViTruyCapService} với phụ thuộc giả (spec 0006, AC-6). */
class PhamViTruyCapServiceTest {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T17:30:00Z"),
			ZoneId.of("Asia/Ho_Chi_Minh"));

	VaiTroQuery vaiTroQuery = mock(VaiTroQuery.class);

	ToaNhaService toaNha = mock(ToaNhaService.class);

	CuTruService cuTru = mock(CuTruService.class);

	PhamViTruyCapService service = new PhamViTruyCapService(this.vaiTroQuery,
			new PhamViTinhToan(this.toaNha, this.cuTru, CLOCK));

	@BeforeEach
	void setUp() {
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
		when(this.toaNha.danhSachMaToaNha()).thenReturn(List.of("A", "B"));
		when(this.cuTru.maCanHoDangCuTru(any(), any())).thenReturn(List.of("A-0101"));
		when(this.vaiTroQuery.cua("ND1")).thenReturn(new VaiTroNguoiDung(List.of(VaiTro.CU_DAN),
				new ChiTietVaiTro(null, null, null, null, null, null, new ChiTietVaiTro.VaiTroCuDan("CD1"))));
	}

	@AfterEach
	void tearDown() {
		RequestContextHolder.resetRequestAttributes();
		SecurityContextHolder.clearContext();
	}

	@Test
	void cuDanCoCanHoTheoNgayVietNamKhongCoToa() {
		dangNhap(VaiTro.CU_DAN);
		PhamViTruyCap phamVi = this.service.cuaNguoiDangGoi();
		assertThat(phamVi.maNguoiDung()).isEqualTo("ND1");
		assertThat(phamVi.vaiTro()).containsExactly(VaiTro.CU_DAN);
		assertThat(phamVi.maToaNha()).isEmpty();
		assertThat(phamVi.maCanHo()).containsExactly("A-0101");
		verify(this.cuTru).maCanHoDangCuTru("CD1", LocalDate.of(2026, 11, 16));
	}

	@Test
	void nhanVienCoMoiToaKhongDocVaiTroQuery() {
		dangNhap(VaiTro.KE_TOAN);
		PhamViTruyCap phamVi = this.service.cuaNguoiDangGoi();
		assertThat(phamVi.maToaNha()).containsExactly("A", "B");
		assertThat(phamVi.maCanHo()).isEmpty();
		verify(this.vaiTroQuery, never()).cua(any());
	}

	@Test
	void quanTriVienKhongCoPhamVi() {
		dangNhap(VaiTro.QUAN_TRI_VIEN);
		PhamViTruyCap phamVi = this.service.cuaNguoiDangGoi();
		assertThat(phamVi.maToaNha()).isEmpty();
		assertThat(phamVi.maCanHo()).isEmpty();
	}

	@Test
	void cuDanKhongConDongCuDanThiKhongCoCanHo() {
		when(this.vaiTroQuery.cua("ND1")).thenReturn(VaiTroNguoiDung.KHONG_CO);
		dangNhap(VaiTro.CU_DAN);
		assertThat(this.service.cuaNguoiDangGoi().maCanHo()).isEmpty();
		verify(this.cuTru, never()).maCanHoDangCuTru(any(), any());
	}

	@Test
	void haiLanTrongMotRequestChiTruyVanMotLan() {
		dangNhap(VaiTro.BAN_QUAN_LY, VaiTro.CU_DAN);
		PhamViTruyCap lan1 = this.service.cuaNguoiDangGoi();
		PhamViTruyCap lan2 = this.service.cuaNguoiDangGoi();
		assertThat(lan2).isSameAs(lan1);
		verify(this.vaiTroQuery, times(1)).cua("ND1");
		verify(this.toaNha, times(1)).danhSachMaToaNha();
		verify(this.cuTru, times(1)).maCanHoDangCuTru(any(), any());
	}

	@Test
	void maNgoaiPhamViGiongMaKhongTonTai() {
		dangNhap(VaiTro.CU_DAN);
		PhamViTruyCap phamVi = this.service.cuaNguoiDangGoi();
		phamVi.yeuCauCanHo("A-0101");
		for (Runnable goi : List.<Runnable>of(() -> phamVi.yeuCauCanHo("A-0201"), () -> phamVi.yeuCauToaNha("A"))) {
			assertThatThrownBy(goi::run).isInstanceOfSatisfying(ApiException.class, ex -> {
				assertThat(ex.getStatus().value()).isEqualTo(404);
				assertThat(ex.getErrorCode()).isEqualTo("NOT_FOUND");
				assertThat(ex.getMessage()).isEqualTo("Không tìm thấy dữ liệu yêu cầu");
			});
		}
		assertThat(phamVi.coVaiTro(VaiTro.CU_DAN)).isTrue();
		assertThat(phamVi.coVaiTro(VaiTro.BAN_QUAN_LY)).isFalse();
	}

	@Test
	void ngoaiRequestThiIllegalState() {
		dangNhap(VaiTro.CU_DAN);
		RequestContextHolder.resetRequestAttributes();
		assertThatThrownBy(this.service::cuaNguoiDangGoi).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void khongPhaiJwtThiIllegalState() {
		SecurityContextHolder.getContext()
			.setAuthentication(UsernamePasswordAuthenticationToken.authenticated("ND1", null, List.of()));
		assertThatThrownBy(this.service::cuaNguoiDangGoi).isInstanceOf(IllegalStateException.class);
	}

	private static void dangNhap(VaiTro... vaiTro) {
		Jwt jwt = Jwt.withTokenValue("t").header("alg", "HS256").subject("ND1").build();
		SecurityContextHolder.getContext()
			.setAuthentication(new JwtAuthenticationToken(jwt,
					Arrays.stream(vaiTro).map(v -> new SimpleGrantedAuthority("ROLE_" + v.name())).toList(), "ND1"));
	}

}
