package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
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

}
