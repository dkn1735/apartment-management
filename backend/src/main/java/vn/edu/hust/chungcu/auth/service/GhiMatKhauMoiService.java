package vn.edu.hust.chungcu.auth.service;

import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.repository.NguoiDungHeThongRepository;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.ErrorCodes;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Phần ghi DB của đổi mật khẩu, trong một transaction: thay hash có điều kiện, rồi thu
 * hồi mọi họ phiên khác của người dùng qua {@link ThuHoiPhienService}. Tách khỏi
 * {@link DoiMatKhauService} để bcrypt chạy ngoài transaction.
 */
@Service
public class GhiMatKhauMoiService {

	private static final Logger log = LoggerFactory.getLogger(GhiMatKhauMoiService.class);

	private final NguoiDungHeThongRepository nguoiDung;

	private final ThuHoiPhienService thuHoiPhien;

	private final NhatKyService nhatKy;

	private final HuyYeuCauDatLaiService huyYeuCauDatLai;

	public GhiMatKhauMoiService(NguoiDungHeThongRepository nguoiDung, ThuHoiPhienService thuHoiPhien,
			NhatKyService nhatKy, HuyYeuCauDatLaiService huyYeuCauDatLai) {
		this.nguoiDung = nguoiDung;
		this.thuHoiPhien = thuHoiPhien;
		this.nhatKy = nhatKy;
		this.huyYeuCauDatLai = huyYeuCauDatLai;
	}

	/**
	 * @param hashCu hash đã dùng để kiểm mật khẩu cũ; DB đã đổi khác thì báo
	 * {@code CONCURRENT_MODIFICATION}, không ghi đè lần đổi kia
	 * @param giuLai họ phiên của máy đang đổi, không bị thu hồi; {@code null} thì thu hồi
	 * hết
	 * @return số họ phiên đã thu hồi
	 */
	@Transactional
	public int ghi(String maNguoiDung, String hashCu, String hashMoi, UUID giuLai, Instant now) {
		if (this.nguoiDung.doiMatKhauHash(maNguoiDung, hashCu, hashMoi, now) == 0) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCodes.CONCURRENT_MODIFICATION,
					"Mật khẩu vừa được thay đổi ở nơi khác, vui lòng tải lại và thử lại.");
		}
		// Dòng tài khoản đã được ghi (đã khóa): link đặt lại qua email cũ không còn dùng
		// được (spec 0010, AC-10)
		this.huyYeuCauDatLai.huyConMo(maNguoiDung, now);
		int soHo = this.thuHoiPhien.thuHoi(maNguoiDung, giuLai, now, LyDoThuHoi.DOI_MAT_KHAU);
		log.info("Đổi mật khẩu: maNguoiDung={}, soPhienThuHoi={}", maNguoiDung, soHo);
		// Đang trong transaction: dòng nhật ký chờ tới sau commit (spec 0007, AC-6)
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_DOI_MAT_KHAU, maNguoiDung,
				DoiTuongTacDong.nguoiDung(maNguoiDung), new ChiTietDoiMatKhau(soHo)));
		return soHo;
	}

}
