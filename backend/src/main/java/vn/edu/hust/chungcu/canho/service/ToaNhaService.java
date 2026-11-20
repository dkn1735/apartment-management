package vn.edu.hust.chungcu.canho.service;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.canho.dto.ToaNhaResponse;
import vn.edu.hust.chungcu.canho.repository.ToaNhaQuery;
import vn.edu.hust.chungcu.canho.repository.ToaNhaRepository;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

/**
 * Phần công khai của phân hệ căn hộ mà phân hệ khác được gọi. Không gọi
 * {@code PhamViTruyCapService}: {@code auth} đã gọi ngược lại service này (spec 0006,
 * Consequences).
 */
@Service
public class ToaNhaService {

	public static final Set<String> TRUONG_SAP_XEP = ToaNhaQuery.COT_SAP_XEP.keySet();

	public static final String SAP_XEP_MAC_DINH = "tenToaNha,asc";

	private final ToaNhaRepository toaNha;

	private final ToaNhaQuery toaNhaQuery;

	public ToaNhaService(ToaNhaRepository toaNha, ToaNhaQuery toaNhaQuery) {
		this.toaNha = toaNha;
		this.toaNhaQuery = toaNhaQuery;
	}

	/** Mã mọi tòa nhà, tăng dần. */
	@Transactional(readOnly = true)
	public List<String> danhSachMaToaNha() {
		return this.toaNha.findAllMaToaNha();
	}

	/** Danh mục tòa nhà trong {@code maToaNha} (phạm vi của người gọi), AC-7. */
	@Transactional(readOnly = true)
	public PageResponse<ToaNhaResponse> danhSach(List<String> maToaNha, TrangYeuCau trang) {
		if (maToaNha.isEmpty()) {
			return PageResponse.of(List.of(), trang, 0);
		}
		long tong = this.toaNhaQuery.dem(maToaNha);
		List<ToaNhaResponse> items = (trang.offset() < tong) ? this.toaNhaQuery.danhSach(maToaNha, trang) : List.of();
		return PageResponse.of(items, trang, tong);
	}

}
