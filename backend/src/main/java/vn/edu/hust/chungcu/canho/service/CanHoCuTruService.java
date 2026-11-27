package vn.edu.hust.chungcu.canho.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.canho.repository.CanHoGhi;
import vn.edu.hust.chungcu.canho.repository.CanHoQuery;

/**
 * Phần của căn hộ mà phân hệ cư trú được gọi (spec 0014, Kiến trúc phân hệ). Chỉ phụ
 * thuộc repository, không phụ thuộc {@link SuaCanHoService}, nên {@code SuaCanHoService}
 * gọi sang {@code cutru} mà không tạo vòng bean. Mọi hàm chạy trong transaction của nơi
 * gọi.
 */
@Service
public class CanHoCuTruService {

	private final CanHoQuery query;

	private final CanHoGhi ghi;

	public CanHoCuTruService(CanHoQuery query, CanHoGhi ghi) {
		this.query = query;
		this.ghi = ghi;
	}

	/** Tòa của căn; rỗng khi căn không tồn tại. */
	public Optional<String> maToaNhaCua(String maCanHo) {
		return this.query.maToaNhaCua(maCanHo);
	}

	/**
	 * Khóa dòng căn ({@code SELECT ... FOR UPDATE}) tới hết transaction và đọc trạng thái
	 * sử dụng hiện tại; rỗng khi căn không tồn tại.
	 */
	public Optional<String> khoaVaDocTrangThai(String maCanHo) {
		return this.ghi.khoaDong(maCanHo).map(CanHoGhi.GiaTri::trangThaiSuDung);
	}

	/**
	 * Mã các căn thuộc tòa trong phạm vi, thu hẹp theo tòa và tầng khi có (spec 0015,
	 * AC-10). Phạm vi rỗng hoặc tòa ngoài phạm vi thì trả tập rỗng mà không truy vấn.
	 * @param maToaNha {@code null} là mọi tòa trong phạm vi
	 * @param soTang {@code null} là mọi tầng
	 */
	public Set<String> maCanHoThuocBoLoc(Collection<String> maToaNhaPhamVi, String maToaNha, Integer soTang) {
		Collection<String> toa = (maToaNha == null) ? maToaNhaPhamVi
				: (maToaNhaPhamVi.contains(maToaNha) ? List.of(maToaNha) : List.of());
		return toa.isEmpty() ? Set.of() : this.query.maCanHoTheoToa(toa, soTang);
	}

	/** Gọi sau {@link #khoaVaDocTrangThai} trong cùng transaction. */
	public void capNhatTrangThaiSuDung(String maCanHo, String trangThaiMoi) {
		this.ghi.capNhatTrangThai(maCanHo, trangThaiMoi);
	}

}
