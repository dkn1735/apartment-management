package vn.edu.hust.chungcu.auth.service;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository.HoTenNguoiDung;

/**
 * Tra cứu người dùng cho phân hệ khác, để không ai đọc bảng của {@code auth} trực tiếp.
 * Chỉ phụ thuộc repository, nên phân hệ mà {@code auth} gọi tới (như {@code nhatky})
 * inject được service này mà không thành vòng phụ thuộc giữa bean.
 */
@Service
public class NguoiDungTraCuuService {

	private final NguoiDungHeThongRepository nguoiDung;

	public NguoiDungTraCuuService(NguoiDungHeThongRepository nguoiDung) {
		this.nguoiDung = nguoiDung;
	}

	/**
	 * Họ tên hiện tại theo mã, bằng một truy vấn (spec 0007, AC-13). Mã không tồn tại thì
	 * không có trong map; tập rỗng thì trả map rỗng mà không truy vấn.
	 */
	@Transactional(readOnly = true)
	public Map<String, String> hoTenTheoMa(Collection<String> maNguoiDung) {
		if (maNguoiDung.isEmpty()) {
			return Map.of();
		}
		return this.nguoiDung.timHoTen(maNguoiDung)
			.stream()
			.collect(Collectors.toUnmodifiableMap(HoTenNguoiDung::getMaNguoiDung, HoTenNguoiDung::getHoTen));
	}

}
