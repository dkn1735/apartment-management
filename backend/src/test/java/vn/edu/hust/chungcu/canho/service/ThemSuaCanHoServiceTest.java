package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.canho.dto.SuaCanHoRequest;
import vn.edu.hust.chungcu.canho.dto.ThemCanHoRequest;
import vn.edu.hust.chungcu.canho.repository.CanHoGhi;
import vn.edu.hust.chungcu.canho.repository.CanHoQuery;
import vn.edu.hust.chungcu.canho.repository.TangQuery;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/** Tạo và sửa căn hộ với repository giả (spec 0012, AC-8, AC-9, AC-12, AC-14). */
class ThemSuaCanHoServiceTest {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final PhamViTruyCap BQL = new PhamViTruyCap("ND00000002", Set.of(VaiTro.BAN_QUAN_LY), List.of("A"),
			List.of());

	@Test
	void tangNgoaiPhamViLaTangKhongTonTai() {
		TangQuery tang = mock(TangQuery.class);
		when(tang.maToaNhaCua("B-01")).thenReturn(Optional.of("B"));
		CanHoQuery query = mock(CanHoQuery.class);
		ThemCanHoService service = new ThemCanHoService(tang, query, mock(CanHoGhi.class), mock(NhatKyService.class));
		assertThatThrownBy(() -> service.them(BQL, them(" b-01 ", "5", "50", null))).isInstanceOfSatisfying(
				ApiException.class,
				ex -> assertThat(ex.getErrors()).extracting(FieldError::message).containsExactly("Tầng không tồn tại"));
		verifyNoInteractions(query);
	}

	@Test
	void khoaChinhKhiChayDuaThanh409() {
		TangQuery tang = mock(TangQuery.class);
		when(tang.maToaNhaCua("A-01")).thenReturn(Optional.of("A"));
		CanHoGhi ghi = mock(CanHoGhi.class);
		NhatKyService nhatKy = mock(NhatKyService.class);
		doThrow(trungKhoa("can_ho_pkey")).when(ghi).them(anyString(), anyString(), any(), anyString());
		ThemCanHoService service = new ThemCanHoService(tang, mock(CanHoQuery.class), ghi, nhatKy);
		assertThatThrownBy(() -> service.them(BQL, them("A-01", "5", "50", null)))
			.isInstanceOfSatisfying(ApiException.class, ex -> {
				assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
				assertThat(ex.getErrorCode()).isEqualTo("CANHO_CAN_HO_DA_TON_TAI");
				assertThat(ex.getErrors()).extracting(FieldError::message)
					.containsExactly("Tầng này đã có căn số 5 (A-0105)");
			});
		verify(nhatKy, never()).ghi(any());
	}

	@Test
	void loiTrungKhacThiNemTiep() {
		TangQuery tang = mock(TangQuery.class);
		when(tang.maToaNhaCua("A-01")).thenReturn(Optional.of("A"));
		CanHoGhi ghi = mock(CanHoGhi.class);
		DuplicateKeyException khac = trungKhoa("uq_khac");
		doThrow(khac).when(ghi).them(anyString(), anyString(), any(), anyString());
		ThemCanHoService service = new ThemCanHoService(tang, mock(CanHoQuery.class), ghi, mock(NhatKyService.class));
		assertThatThrownBy(() -> service.them(BQL, them("A-01", "5", "50", null))).isSameAs(khac);
	}

	@Test
	void suaThieuTrangThaiVaDienTichSai() {
		assertThatThrownBy(() -> SuaCanHoService.kiem(new SuaCanHoRequest(JSON.readTree("75.505"), null)))
			.isInstanceOfSatisfying(ApiException.class,
					ex -> assertThat(ex.getErrors()).extracting(FieldError::message)
						.containsExactly("Diện tích phải lớn hơn 0, tối đa 1000 m² và tối đa 2 chữ số thập phân",
								"Vui lòng chọn trạng thái sử dụng"));
	}

	@Test
	void thayDoiChiGomTruongDoi() {
		CanHoGhi.GiaTri cu = new CanHoGhi.GiaTri(new BigDecimal("75.50"), "Trống");
		assertThat(SuaCanHoService.thayDoi(cu, new CanHoGhi.GiaTri(new BigDecimal("75.5"), "Trống"))).isNull();
		ChiTietCanHo.Sua chiDienTich = SuaCanHoService.thayDoi(cu,
				new CanHoGhi.GiaTri(new BigDecimal("80.00"), "Trống"));
		assertThat(chiDienTich.trangThaiSuDung()).isNull();
		assertThat(chiDienTich.dienTich().cu()).hasToString("75.50");
		assertThat(chiDienTich.dienTich().moi()).hasToString("80.00");
		ChiTietCanHo.Sua chiTrangThai = SuaCanHoService.thayDoi(cu,
				new CanHoGhi.GiaTri(new BigDecimal("75.50"), "Đã bàn giao"));
		assertThat(chiTrangThai.dienTich()).isNull();
		assertThat(chiTrangThai.trangThaiSuDung()).isEqualTo(new ChiTietCanHo.ThayDoi<>("Trống", "Đã bàn giao"));
		assertThat(JSON.writeValueAsString(chiTrangThai))
			.isEqualTo("{\"trangThaiSuDung\":{\"cu\":\"Trống\",\"moi\":\"Đã bàn giao\"}}");
	}

	@Test
	void suaNgoaiPhamViThi404KhongKhoaDong() {
		CanHoQuery query = mock(CanHoQuery.class);
		when(query.maToaNhaCua("B-0101")).thenReturn(Optional.of("B"));
		CanHoGhi ghi = mock(CanHoGhi.class);
		SuaCanHoService service = new SuaCanHoService(query, ghi, mock(NhatKyService.class));
		assertThatThrownBy(
				() -> service.sua(BQL, "b-0101", new SuaCanHoRequest(JSON.readTree("50"), JSON.valueToTree("Trống"))))
			.isInstanceOfSatisfying(ApiException.class,
					ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND));
		verifyNoInteractions(ghi);
	}

	private static ThemCanHoRequest them(String maTang, String soCan, String dienTich, String trangThai) {
		return new ThemCanHoRequest(JSON.valueToTree(maTang), JSON.readTree(soCan), JSON.readTree(dienTich),
				(trangThai != null) ? JSON.valueToTree(trangThai) : null);
	}

	private static DuplicateKeyException trungKhoa(String rangBuoc) {
		ServerErrorMessage msg = new ServerErrorMessage("SERROR\0C23505\0Mduplicate key\0n" + rangBuoc + "\0\0");
		return new DuplicateKeyException("trùng", new PSQLException(msg));
	}

}
