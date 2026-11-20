package vn.edu.hust.chungcu.common.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorResponse;

/** Đọc tham số phân trang (spec 0006, AC-9). */
class TrangYeuCauTest {

	private static final Set<String> SORT = Set.of("maToaNha", "tenToaNha");

	@Test
	void thieuThamSoThiDungMacDinh() {
		TrangYeuCau trang = TrangYeuCau.tu(null, "", null, SORT, "tenToaNha,asc");
		assertThat(trang).isEqualTo(new TrangYeuCau(1, 20, "tenToaNha", Sort.Direction.ASC));
		assertThat(trang.offset()).isZero();
	}

	@Test
	void docDungGiaTri() {
		TrangYeuCau trang = TrangYeuCau.tu("3", "100", "maToaNha,desc", SORT, "tenToaNha,asc");
		assertThat(trang).isEqualTo(new TrangYeuCau(3, 100, "maToaNha", Sort.Direction.DESC));
		assertThat(trang.offset()).isEqualTo(200);
	}

	@Test
	void gomMoiLoiTheoThuTuPageSizeSort() {
		assertThatThrownBy(() -> TrangYeuCau.tu("0", "101", "tenToaNha,ASC", SORT, "tenToaNha,asc"))
			.isInstanceOfSatisfying(ApiException.class, ex -> {
				assertThat(ex.getErrorCode()).isEqualTo("VALIDATION_FAILED");
				assertThat(ex.getErrors()).extracting(ErrorResponse.FieldError::field)
					.containsExactly("page", "size", "sort");
			});
	}

	@Test
	void giaTriKhongPhaiSoHoacSaiDangDeuLa400() {
		for (List<String> sai : List.of(List.of("abc", "20", "maToaNha,asc"), List.of(" 1", "20", "maToaNha,asc"),
				List.of("99999999999", "20", "maToaNha,asc"), List.of("1", "+5", "maToaNha,asc"),
				List.of("1", "20", "soTang,asc"), List.of("1", "20", "maToaNha,asc,x"))) {
			assertThatThrownBy(() -> TrangYeuCau.tu(sai.get(0), sai.get(1), sai.get(2), SORT, "tenToaNha,asc"))
				.isInstanceOf(ApiException.class);
		}
	}

	@Test
	void pageResponseTinhTongSoTrang() {
		TrangYeuCau trang = new TrangYeuCau(1, 20, "maToaNha", Sort.Direction.ASC);
		assertThat(PageResponse.of(List.of(), trang, 0).totalPages()).isZero();
		assertThat(PageResponse.of(List.of(), trang, 20).totalPages()).isEqualTo(1);
		assertThat(PageResponse.of(List.of(), trang, 21).totalPages()).isEqualTo(2);
	}

}
