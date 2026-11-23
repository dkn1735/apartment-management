package vn.edu.hust.chungcu.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import vn.edu.hust.chungcu.auth.dto.ChiTietVaiTro;
import vn.edu.hust.chungcu.auth.dto.HoSoResponse;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTroNguoiDung;
import vn.edu.hust.chungcu.auth.entity.NguoiDungHeThong;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.auth.repository.VaiTroQuery;
import vn.edu.hust.chungcu.canho.service.ToaNhaService;
import vn.edu.hust.chungcu.common.config.AppProperties;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.cutru.service.CuTruService;

/** Nhánh phạm vi và khóa của {@link HoSoService}, với phụ thuộc giả (AC-8, AC-9). */
class HoSoServiceTest {

	private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");

	// 00:30 ngày 16/11 giờ Việt Nam, vẫn là 15/11 theo UTC
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-15T17:30:00Z"), VN);

	private static final ChiTietVaiTro.VaiTroCuDan CU_DAN = new ChiTietVaiTro.VaiTroCuDan("CD00000001");

	private static final ChiTietVaiTro.VaiTroBanQuanLy BQL = new ChiTietVaiTro.VaiTroBanQuanLy("BQL00000001",
			"Trưởng ban", "Vận hành");

	NguoiDungHeThongRepository nguoiDung = mock(NguoiDungHeThongRepository.class);

	VaiTroQuery vaiTroQuery = mock(VaiTroQuery.class);

	ToaNhaService toaNha = mock(ToaNhaService.class);

	CuTruService cuTru = mock(CuTruService.class);

	HoSoService service;

	@BeforeEach
	void setUp() {
		AppProperties properties = new AppProperties(VN,
				new AppProperties.Security("k".repeat(32), Duration.ofMinutes(15), Duration.ofDays(7), 4, true,
						Duration.ofDays(30), Duration.ofSeconds(10), Duration.ofDays(30), "0 0 3 * * *",
						Duration.ofMinutes(30)),
				"http://localhost:3000", new AppProperties.Mail("chungcu@localhost"));
		this.service = new HoSoService(this.nguoiDung, this.vaiTroQuery,
				new PhamViTinhToan(this.toaNha, this.cuTru, CLOCK), new AuthCookies(properties));
		when(this.toaNha.danhSachMaToaNha()).thenReturn(List.of("A", "B"));
		when(this.cuTru.maCanHoDangCuTru(any(), any())).thenReturn(List.of("A-0101"));
	}

	@Test
	void nhanVienCoMoiToaKhongCoCanHo() {
		HoSoResponse hoSo = hoSo("Hoạt động", vaiTro(List.of(VaiTro.BAN_QUAN_LY), BQL, null));
		assertThat(hoSo.phamVi().maToaNha()).containsExactly("A", "B");
		assertThat(hoSo.phamVi().maCanHo()).isEmpty();
		verify(this.cuTru, never()).maCanHoDangCuTru(any(), any());
	}

	@Test
	void cuDanCoCanHoTheoNgayVietNam() {
		HoSoResponse hoSo = hoSo("Hoạt động", vaiTro(List.of(VaiTro.CU_DAN), null, CU_DAN));
		assertThat(hoSo.phamVi().maToaNha()).isEmpty();
		assertThat(hoSo.phamVi().maCanHo()).containsExactly("A-0101");
		verify(this.cuTru).maCanHoDangCuTru("CD00000001", LocalDate.of(2026, 11, 16));
		verify(this.toaNha, never()).danhSachMaToaNha();
	}

	@Test
	void haiVaiTroCoCaHaiPhamVi() {
		HoSoResponse hoSo = hoSo("Hoạt động", vaiTro(List.of(VaiTro.BAN_QUAN_LY, VaiTro.CU_DAN), BQL, CU_DAN));
		assertThat(hoSo.phamVi().maToaNha()).containsExactly("A", "B");
		assertThat(hoSo.phamVi().maCanHo()).containsExactly("A-0101");
	}

	// Spec 0006 AC-6: Admin không có phạm vi tòa (lệch có chủ ý so với spec 0004 AC-8)
	@Test
	void chiQuanTriVienThiKhongCoToa() {
		HoSoResponse hoSo = hoSo("Hoạt động", vaiTro(List.of(VaiTro.QUAN_TRI_VIEN), null, null));
		assertThat(hoSo.phamVi().maToaNha()).isEmpty();
		assertThat(hoSo.phamVi().maCanHo()).isEmpty();
		verify(this.toaNha, never()).danhSachMaToaNha();
	}

	@Test
	void khongCoVaiTroThiPhamViRong() {
		HoSoResponse hoSo = hoSo("Hoạt động", VaiTroNguoiDung.KHONG_CO);
		assertThat(hoSo.vaiTro()).isEmpty();
		assertThat(hoSo.phamVi().maToaNha()).isEmpty();
		assertThat(hoSo.phamVi().maCanHo()).isEmpty();
	}

	@Test
	void taiKhoanKhoaTra403KemXoaCookie() {
		when(this.nguoiDung.findById("ND1")).thenReturn(Optional.of(nguoiDung("Tạm khóa")));
		assertThatThrownBy(() -> this.service.cua("ND1")).isInstanceOfSatisfying(ApiException.class, ex -> {
			assertThat(ex.getErrorCode()).isEqualTo("AUTH_ACCOUNT_LOCKED");
			assertThat(ex.getHeaders().get(HttpHeaders.SET_COOKIE)).hasSize(3)
				.allSatisfy(cookie -> assertThat(cookie).contains("Max-Age=0"));
		});
	}

	@Test
	void nguoiDungKhongTonTaiTra401KemXoaCookie() {
		when(this.nguoiDung.findById("ND1")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> this.service.cua("ND1")).isInstanceOfSatisfying(ApiException.class, ex -> {
			assertThat(ex.getErrorCode()).isEqualTo("UNAUTHENTICATED");
			assertThat(ex.getHeaders().get(HttpHeaders.SET_COOKIE)).hasSize(3);
		});
	}

	private HoSoResponse hoSo(String trangThai, VaiTroNguoiDung vaiTro) {
		when(this.nguoiDung.findById("ND1")).thenReturn(Optional.of(nguoiDung(trangThai)));
		when(this.vaiTroQuery.cua("ND1")).thenReturn(vaiTro);
		return this.service.cua("ND1");
	}

	private static VaiTroNguoiDung vaiTro(List<VaiTro> ma, ChiTietVaiTro.VaiTroBanQuanLy bql,
			ChiTietVaiTro.VaiTroCuDan cuDan) {
		return new VaiTroNguoiDung(ma, new ChiTietVaiTro(null, bql, null, null, null, null, cuDan));
	}

	private static NguoiDungHeThong nguoiDung(String trangThai) {
		NguoiDungHeThong nd = org.springframework.beans.BeanUtils.instantiateClass(NguoiDungHeThong.class);
		ReflectionTestUtils.setField(nd, "maNguoiDung", "ND1");
		ReflectionTestUtils.setField(nd, "hoTen", "Người Thử");
		ReflectionTestUtils.setField(nd, "trangThaiTaiKhoan", trangThai);
		return nd;
	}

}
