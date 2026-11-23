package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.canho.dto.ThemTangRequest;
import vn.edu.hust.chungcu.canho.repository.TangQuery;
import vn.edu.hust.chungcu.canho.repository.ToaNhaGhi;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/** Thứ tự kiểm khi thêm tầng (spec 0011, AC-4, AC-5). */
class TangServiceTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final ToaNhaGhi ghi = mock(ToaNhaGhi.class);

	private final NhatKyService nhatKy = mock(NhatKyService.class);

	private final TangService service = new TangService(mock(TangQuery.class), this.ghi, this.nhatKy);

	private final PhamViTruyCap phamVi = new PhamViTruyCap("ND00000001", Set.of(VaiTro.BAN_QUAN_LY), List.of("A"),
			List.of());

	private static ThemTangRequest body(String json) {
		return new ThemTangRequest(JSON.readTree(json));
	}

	@Test
	void soTangSaiThi400TruocPhamVi() {
		assertThatThrownBy(() -> this.service.them(this.phamVi, "KHONGCO", body("0"))).isInstanceOfSatisfying(
				ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
		assertThatThrownBy(() -> this.service.them(this.phamVi, "A", new ThemTangRequest(null))).isInstanceOfSatisfying(
				ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
		verifyNoInteractions(this.ghi);
	}

	@Test
	void ngoaiPhamViThi404KhongKhoa() {
		assertThatThrownBy(() -> this.service.them(this.phamVi, "B", body("4"))).isInstanceOfSatisfying(
				ApiException.class, ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
		verifyNoInteractions(this.ghi);
	}

	@Test
	void tranTangThangTangKeTiep() {
		when(this.ghi.soTangKhoaDong("A")).thenReturn(Optional.of(99));
		assertThatThrownBy(() -> this.service.them(this.phamVi, "A", body("50")))
			.isInstanceOfSatisfying(ApiException.class, ex -> {
				assertThat(ex.getErrorCode()).isEqualTo("CANHO_TANG_SAI_THU_TU");
				assertThat(ex.getErrors().get(0).message()).isEqualTo("Tòa đã đạt tối đa 99 tầng");
			});
		verify(this.ghi, never()).themTang(any(), any(Integer.class), any());
	}

	@Test
	void tangKeTiepDungThiGhiVaNhatKy() {
		when(this.ghi.soTangKhoaDong("A")).thenReturn(Optional.of(3));
		assertThat(this.service.them(this.phamVi, "A", body("4")).maTang()).isEqualTo("A-04");
		verify(this.ghi).themTang("A", 4, "A-04");
		verify(this.nhatKy).ghi(any());
	}

}
