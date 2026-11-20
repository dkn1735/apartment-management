package vn.edu.hust.chungcu.canho;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import vn.edu.hust.chungcu.canho.dto.ToaNhaResponse;
import vn.edu.hust.chungcu.canho.repository.ToaNhaQuery;
import vn.edu.hust.chungcu.canho.repository.ToaNhaRepository;
import vn.edu.hust.chungcu.canho.service.ToaNhaService;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

/** Phạm vi rỗng cho trang rỗng mà không truy vấn bảng (spec 0006, AC-7). */
class ToaNhaServiceTest {

	@Test
	void phamViRongKhongTruyVan() {
		ToaNhaQuery query = mock(ToaNhaQuery.class);
		ToaNhaService service = new ToaNhaService(mock(ToaNhaRepository.class), query);
		PageResponse<ToaNhaResponse> trang = service.danhSach(List.of(),
				new TrangYeuCau(1, 20, "tenToaNha", Sort.Direction.ASC));
		assertThat(trang.items()).isEmpty();
		assertThat(trang.totalItems()).isZero();
		assertThat(trang.totalPages()).isZero();
		verifyNoInteractions(query);
	}

}
