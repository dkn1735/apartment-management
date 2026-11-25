package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.canho.dto.ThemToaNhaRequest;
import vn.edu.hust.chungcu.canho.repository.ToaNhaGhi;
import vn.edu.hust.chungcu.canho.repository.ToaNhaQuery;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/** Chuẩn hóa và kiểm dữ liệu thêm tòa (spec 0011, AC-1, AC-2). */
class ThemToaNhaServiceTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Test
	void chuanHoaTruocKhiKiem() {
		ThemToaNhaService.ToaNhaMoi moi = ThemToaNhaService
			.kiem(new ThemToaNhaRequest(" b2 ", "  Tòa \t  B2 ", JSON.readTree("12"), " Khu\n\nmới "));
		assertThat(moi).isEqualTo(new ThemToaNhaService.ToaNhaMoi("B2", "Tòa B2", 12, "Khu mới"));
	}

	@Test
	void loi400KhongKiemTrung() {
		ToaNhaQuery query = mock(ToaNhaQuery.class);
		ToaNhaGhi ghi = mock(ToaNhaGhi.class);
		ThemToaNhaService service = new ThemToaNhaService(query, ghi, mock(NhatKyService.class));
		assertThatThrownBy(() -> service.them("ND1", new ThemToaNhaRequest("A", "   ", JSON.readTree("3"), "Khu")))
			.isInstanceOfSatisfying(ApiException.class,
					ex -> assertThat(ex.getErrors()).extracting(FieldError::field).containsExactly("tenToaNha"));
		verifyNoInteractions(query, ghi);
	}

	// covers: AC-3 (ràng buộc khóa chính chặn khi chạy đua: 409 mục maToaNha, không thêm
	// tầng)
	@Test
	void rangBuocKhoaChinhThanh409MaToaNha() {
		ToaNhaGhi ghi = mock(ToaNhaGhi.class);
		NhatKyService nhatKy = mock(NhatKyService.class);
		doThrow(viPham("toa_nha_pkey")).when(ghi).themToaNha(anyString(), anyString(), anyInt(), anyString());
		ThemToaNhaService service = new ThemToaNhaService(khongTrung(), ghi, nhatKy);
		assertThatThrownBy(() -> service.them("ND1", hopLe())).isInstanceOfSatisfying(ApiException.class, ex -> {
			assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(ex.getErrorCode()).isEqualTo("CANHO_TOA_NHA_DA_TON_TAI");
			assertThat(ex.getErrors()).containsExactly(new FieldError("maToaNha", "Mã tòa nhà đã được dùng"));
		});
		verify(ghi, never()).themTangTu1Den(anyString(), anyInt());
		verifyNoInteractions(nhatKy);
	}

	// covers: AC-3 (tên ràng buộc lạ thì ném lại lỗi gốc)
	@Test
	void rangBuocLaThiNemLaiLoiGoc() {
		ToaNhaGhi ghi = mock(ToaNhaGhi.class);
		DataIntegrityViolationException goc = viPham("chk_toa_nha_so_tang");
		doThrow(goc).when(ghi).themToaNha(anyString(), anyString(), anyInt(), anyString());
		ThemToaNhaService service = new ThemToaNhaService(khongTrung(), ghi, mock(NhatKyService.class));
		assertThatThrownBy(() -> service.them("ND1", hopLe())).isSameAs(goc);
	}

	// covers: AC-3 (không đọc được tên ràng buộc thì ném lại lỗi gốc)
	@Test
	void khongDocDuocTenRangBuocThiNemLaiLoiGoc() {
		ToaNhaGhi ghi = mock(ToaNhaGhi.class);
		DataIntegrityViolationException goc = new DataIntegrityViolationException("insert");
		doThrow(goc).when(ghi).themToaNha(anyString(), anyString(), anyInt(), anyString());
		ThemToaNhaService service = new ThemToaNhaService(khongTrung(), ghi, mock(NhatKyService.class));
		assertThatThrownBy(() -> service.them("ND1", hopLe())).isSameAs(goc);
	}

	private static ToaNhaQuery khongTrung() {
		ToaNhaQuery query = mock(ToaNhaQuery.class);
		when(query.trung(anyString(), anyString())).thenReturn(new ToaNhaQuery.Trung(false, false));
		return query;
	}

	private static ThemToaNhaRequest hopLe() {
		return new ThemToaNhaRequest("B", "Tòa B", JSON.readTree("5"), "Khu");
	}

	private static DataIntegrityViolationException viPham(String rangBuoc) {
		return new DataIntegrityViolationException("insert",
				new PSQLException(new ServerErrorMessage("SERROR\0C23505\0Mduplicate key\0n" + rangBuoc + "\0")));
	}

}
