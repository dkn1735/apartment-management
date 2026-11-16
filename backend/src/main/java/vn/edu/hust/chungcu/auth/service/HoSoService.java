package vn.edu.hust.chungcu.auth.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.HoSoResponse;
import vn.edu.hust.chungcu.auth.dto.VaiTroNguoiDung;
import vn.edu.hust.chungcu.auth.entity.NguoiDungHeThong;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.auth.repository.VaiTroQuery;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;

/**
 * Hồ sơ của người đang đăng nhập (spec 0004, mục Hồ sơ). Vai trò luôn đọc từ DB, không
 * lấy từ token.
 */
@Service
public class HoSoService {

	private final NguoiDungHeThongRepository nguoiDung;

	private final VaiTroQuery vaiTroQuery;

	public HoSoService(NguoiDungHeThongRepository nguoiDung, VaiTroQuery vaiTroQuery) {
		this.nguoiDung = nguoiDung;
		this.vaiTroQuery = vaiTroQuery;
	}

	@Transactional(readOnly = true)
	public HoSoResponse cua(String maNguoiDung) {
		NguoiDungHeThong tk = this.nguoiDung.findById(maNguoiDung)
			.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED,
					"Bạn cần đăng nhập để tiếp tục"));
		VaiTroNguoiDung vaiTro = this.vaiTroQuery.cua(maNguoiDung);
		return new HoSoResponse(tk.getMaNguoiDung(), tk.getHoTen(), tk.getSoDienThoai(), tk.getEmail(),
				tk.getTrangThaiTaiKhoan(), vaiTro.vaiTro());
	}

}
