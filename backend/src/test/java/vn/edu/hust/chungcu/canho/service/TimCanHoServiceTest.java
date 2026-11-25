package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.canho.dto.BoLocCanHo;
import vn.edu.hust.chungcu.canho.dto.CanHoResponse;
import vn.edu.hust.chungcu.canho.repository.CanHoQuery;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/** Kiểm bộ lọc và hai luật quyền xem chi tiết (spec 0012, AC-2, AC-3, AC-6). */
class TimCanHoServiceTest {

	private static final CanHoResponse A0201 = new CanHoResponse("A-0201", "A-02", 2, "A", "Tòa A",
			new BigDecimal("75.50"), "Đang sử dụng", false);

	@Test
	void chuanHoaBoLoc() {
		List<FieldError> loi = new ArrayList<>();
		TimCanHoService.BoLocDaKiem loc = TimCanHoService.kiem(new BoLocCanHo(" a ", " 2 ", "50", "",
				Arrays.asList(" Trống ", "", null, "Trống", "Đã bàn giao"), " a-01 "), loi);
		assertThat(loi).isEmpty();
		assertThat(loc).isEqualTo(new TimCanHoService.BoLocDaKiem("A", 2, new BigDecimal("50"), null,
				List.of("Trống", "Đã bàn giao"), "A-01"));
	}

	@Test
	void mucLoiTheoThuTu() {
		List<FieldError> loi = new ArrayList<>();
		TimCanHoService.kiem(new BoLocCanHo(null, "2", "80", "50", List.of("X", "Y"), "x".repeat(21)), loi);
		assertThat(loi).extracting(FieldError::field).containsExactly("soTang", "dienTichDen", "trangThaiSuDung", "q");
	}

	@Test
	void phamViRongKhongChaySql() {
		CanHoQuery query = mock(CanHoQuery.class);
		TimCanHoService service = new TimCanHoService(query);
		var trang = service.timKiem(new BoLocCanHo(null, null, null, null, null, null), null, null, null,
				phamVi(Set.of(VaiTro.BAN_QUAN_LY), List.of(), List.of()));
		assertThat(trang.totalItems()).isZero();
		verifyNoInteractions(query);
	}

	@Test
	void cuDanXemCanCuaMinhNhanVienXemTheoToa() {
		CanHoQuery query = mock(CanHoQuery.class);
		when(query.cua(any())).thenReturn(Optional.of(A0201));
		TimCanHoService service = new TimCanHoService(query);
		assertThat(service.chiTiet("a-0201", phamVi(Set.of(VaiTro.CU_DAN), List.of(), List.of("A-0201"))))
			.isEqualTo(A0201);
		assertThat(service.chiTiet("A-0201", phamVi(Set.of(VaiTro.KE_TOAN), List.of("A"), List.of()))).isEqualTo(A0201);
		assertThat(service.chiTiet("A-0201",
				phamVi(Set.of(VaiTro.CU_DAN, VaiTro.BAN_QUAN_TRI), List.of("A"), List.of("A-0101"))))
			.isEqualTo(A0201);
		khongThay(service, phamVi(Set.of(VaiTro.CU_DAN), List.of("A"), List.of("A-0101")));
		khongThay(service, phamVi(Set.of(VaiTro.BAN_QUAN_LY), List.of("B"), List.of()));
		khongThay(service, phamVi(Set.of(VaiTro.KY_THUAT), List.of("A"), List.of("A-0201")));
	}

	private static void khongThay(TimCanHoService service, PhamViTruyCap phamVi) {
		assertThatThrownBy(() -> service.chiTiet("A-0201", phamVi)).isInstanceOfSatisfying(ApiException.class,
				ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	private static PhamViTruyCap phamVi(Set<VaiTro> vaiTro, List<String> toa, List<String> can) {
		return new PhamViTruyCap("ND00000001", vaiTro, toa, can);
	}

}
