package vn.edu.hust.chungcu.auth.service;

import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository;

/**
 * Hủy mọi link đặt lại mật khẩu còn mở của một tài khoản (spec 0010, AC-10). Gọi trong
 * transaction có sẵn của luồng đổi mật khẩu, khóa hay sửa email, sau khi luồng đó đã ghi
 * dòng tài khoản (thứ tự khóa). Chỉ phụ thuộc repository, để các luồng đó không kéo theo
 * luồng email.
 */
@Service
public class HuyYeuCauDatLaiService {

	private final QuenMatKhauRepository repository;

	public HuyYeuCauDatLaiService(QuenMatKhauRepository repository) {
		this.repository = repository;
	}

	/**
	 * @return số yêu cầu đã hủy
	 */
	@Transactional(propagation = Propagation.MANDATORY)
	public int huyConMo(String maNguoiDung, Instant now) {
		return this.repository.huyConMo(maNguoiDung, now);
	}

}
