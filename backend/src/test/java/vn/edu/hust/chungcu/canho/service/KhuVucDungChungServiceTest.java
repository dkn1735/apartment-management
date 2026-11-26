package vn.edu.hust.chungcu.canho.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.canho.dto.KhuVucDungChungResponse;
import vn.edu.hust.chungcu.canho.repository.KhuVucDungChungQuery;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/** Bộ lọc và phạm vi của danh mục khu vực dùng chung (spec 0011, AC-11). */
class KhuVucDungChungServiceTest {

	private static PhamViTruyCap phamVi(List<String> toa) {
		return new PhamViTruyCap("ND1", Set.of(VaiTro.BAN_QUAN_LY), toa, List.of());
	}

	// covers: AC-11 (phạm vi rỗng trả trang rỗng)
	@Test
	void phamViRongTraTrangRongKhongTruyVan() {
		KhuVucDungChungQuery query = mock(KhuVucDungChungQuery.class);
		PageResponse<KhuVucDungChungResponse> trang = new KhuVucDungChungService(query).danhSach(phamVi(List.of()),
				null, null, null, null, null);
		assertThat(trang.items()).isEmpty();
		assertThat(trang.totalItems()).isZero();
		verifyNoInteractions(query);
	}

	// covers: AC-11 (q chuẩn hóa chữ trước khi lọc, rỗng sau chuẩn hóa thì không lọc)
	@Test
	void tuKhoaChuanHoaTruocKhiLoc() {
		KhuVucDungChungQuery query = mock(KhuVucDungChungQuery.class);
		KhuVucDungChungService service = new KhuVucDungChungService(query);
		service.danhSach(phamVi(List.of("A")), null, "  Sảnh   chính ", null, null, null);
		verify(query).dem(List.of("A"), "Sảnh chính");
		service.danhSach(phamVi(List.of("A")), null, " \t ", null, null, null);
		verify(query).dem(eq(List.of("A")), isNull());
	}

	// covers: AC-11 (đúng 100 ký tự vẫn hợp lệ, đếm theo code point)
	@Test
	void tuKhoa100CodePointVanHopLe() {
		KhuVucDungChungQuery query = mock(KhuVucDungChungQuery.class);
		String tuKhoa = "🏢".repeat(100);
		new KhuVucDungChungService(query).danhSach(phamVi(List.of("A")), null, tuKhoa, null, null, null);
		verify(query).dem(List.of("A"), tuKhoa);
	}

	// covers: AC-11 (quá 100 ký tự là 400, kiểm trước phạm vi)
	@Test
	void tuKhoaQua100KyTuLa400TruocPhamVi() {
		KhuVucDungChungQuery query = mock(KhuVucDungChungQuery.class);
		KhuVucDungChungService service = new KhuVucDungChungService(query);
		assertThatThrownBy(() -> service.danhSach(phamVi(List.of("A")), "KHONGCO", "x".repeat(101), null, null, null))
			.isInstanceOfSatisfying(ApiException.class, ex -> {
				assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
				assertThat(ex.getErrors()).containsExactly(new FieldError("q", "Từ khóa tối đa 100 ký tự"));
			});
		verifyNoInteractions(query);
	}

	// covers: AC-11 (maToaNha đổi chữ hoa và thu hẹp phạm vi về đúng tòa đó)
	@Test
	void maToaNhaDoiHoaVaThuHepPhamVi() {
		KhuVucDungChungQuery query = mock(KhuVucDungChungQuery.class);
		when(query.dem(List.of("B"), null)).thenReturn(0L);
		new KhuVucDungChungService(query).danhSach(phamVi(List.of("A", "B")), "b", null, null, null, null);
		verify(query).dem(List.of("B"), null);
	}

	// covers: AC-11 (maToaNha ngoài phạm vi là 404 NOT_FOUND)
	@Test
	void maToaNhaNgoaiPhamViLa404() {
		KhuVucDungChungQuery query = mock(KhuVucDungChungQuery.class);
		KhuVucDungChungService service = new KhuVucDungChungService(query);
		assertThatThrownBy(() -> service.danhSach(phamVi(List.of("A")), "B", null, null, null, null))
			.isInstanceOfSatisfying(ApiException.class, ex -> {
				assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(ex.getErrorCode()).isEqualTo("NOT_FOUND");
			});
		verifyNoInteractions(query);
	}

	// covers: AC-11 (trang vượt quá tổng thì không đọc danh sách)
	@Test
	void trangVuotQuaKhongDocDanhSach() {
		KhuVucDungChungQuery query = mock(KhuVucDungChungQuery.class);
		when(query.dem(List.of("A"), null)).thenReturn(3L);
		PageResponse<KhuVucDungChungResponse> trang = new KhuVucDungChungService(query).danhSach(phamVi(List.of("A")),
				null, null, "2", "20", null);
		assertThat(trang.items()).isEmpty();
		assertThat(trang.totalItems()).isEqualTo(3);
		verify(query, org.mockito.Mockito.never()).danhSach(any(), any(), any());
	}

}
