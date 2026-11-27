package vn.edu.hust.chungcu.cutru.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.cutru.entity.HoSoCuTru;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruRepository;

/** Phần công khai của phân hệ cư trú mà phân hệ khác được gọi. */
@Service
public class CuTruService {

	private final HoSoCuTruRepository hoSoCuTru;

	public CuTruService(HoSoCuTruRepository hoSoCuTru) {
		this.hoSoCuTru = hoSoCuTru;
	}

	/**
	 * Mã các căn hộ mà cư dân đang cư trú vào {@code ngay}: hồ sơ {@code Hợp lệ}, đã bắt
	 * đầu và chưa kết thúc. Không trùng, tăng dần.
	 */
	@Transactional(readOnly = true)
	public List<String> maCanHoDangCuTru(String maCuDan, LocalDate ngay) {
		return this.hoSoCuTru.findMaCanHoConHieuLuc(maCuDan, HoSoCuTru.HOP_LE, ngay);
	}

	/**
	 * Căn có người đang cư trú vào {@code ngay} (spec 0014, AC-10). Gọi dưới khóa dòng
	 * căn để kết quả không đổi tới hết transaction.
	 */
	@Transactional(readOnly = true)
	public boolean coNguoiDangCuTru(String maCanHo, LocalDate ngay) {
		return this.hoSoCuTru.coHoSoConHieuLuc(maCanHo, HoSoCuTru.HOP_LE, ngay);
	}

}
