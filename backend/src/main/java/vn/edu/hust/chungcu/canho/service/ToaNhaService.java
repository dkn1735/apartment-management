package vn.edu.hust.chungcu.canho.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.canho.repository.ToaNhaRepository;

/** Phần công khai của phân hệ căn hộ mà phân hệ khác được gọi. */
@Service
public class ToaNhaService {

	private final ToaNhaRepository toaNha;

	public ToaNhaService(ToaNhaRepository toaNha) {
		this.toaNha = toaNha;
	}

	/** Mã mọi tòa nhà, tăng dần. */
	@Transactional(readOnly = true)
	public List<String> danhSachMaToaNha() {
		return this.toaNha.findAllMaToaNha();
	}

}
