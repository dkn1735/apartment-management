package vn.edu.hust.chungcu.auth.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.repository.PhienDangNhapRepository;

/**
 * Thu hồi mọi họ phiên còn hiệu lực của một người dùng, mỗi họ dưới khóa của nó (spec
 * 0005, Key invariants), để một lần làm mới đang chạy không lọt ra token mới. Dùng chung
 * cho đổi mật khẩu, khóa tài khoản và đặt lại mật khẩu (spec 0008).
 */
@Service
public class ThuHoiPhienService {

	private final PhienDangNhapRepository phien;

	public ThuHoiPhienService(PhienDangNhapRepository phien) {
		this.phien = phien;
	}

	/**
	 * @param giuLai họ phiên không thu hồi; {@code null} thì thu hồi hết
	 * @param lyDo giá trị của {@link LyDoThuHoi}
	 * @return số họ phiên đã thu hồi
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public int thuHoi(String maNguoiDung, UUID giuLai, Instant now, String lyDo) {
		List<UUID> ho = this.phien.hoConHieuLucCua(maNguoiDung, now).stream().filter((h) -> !h.equals(giuLai)).toList();
		int soHo = 0;
		// Khóa theo thứ tự cố định của truy vấn để hai lần thu hồi cùng lúc không
		// deadlock
		for (UUID h : ho) {
			this.phien.khoaHo(h);
			if (this.phien.thuHoiHo(h, now, lyDo) > 0) {
				soHo++;
			}
		}
		return soHo;
	}

}
