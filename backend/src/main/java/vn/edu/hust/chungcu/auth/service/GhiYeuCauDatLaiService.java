package vn.edu.hust.chungcu.auth.service;

import java.time.Duration;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository.TaiKhoanNhanEmail;
import vn.edu.hust.chungcu.auth.repository.QuenMatKhauRepository.ThongKeYeuCau;
import vn.edu.hust.chungcu.common.config.AppProperties;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Transaction của một yêu cầu đặt lại mật khẩu (spec 0010, Luồng yêu cầu bước 2): khóa
 * dòng tài khoản để các yêu cầu song song xếp hàng, kiểm giới hạn, hủy link cũ, chèn link
 * mới, rồi phát sự kiện để gửi email sau commit. Giữ ngắn: không gọi SMTP ở đây.
 */
@Service
public class GhiYeuCauDatLaiService {

	private final QuenMatKhauRepository repository;

	private final RefreshTokenGenerator tokens;

	private final NhatKyService nhatKy;

	private final ApplicationEventPublisher events;

	private final Duration thoiHan;

	public GhiYeuCauDatLaiService(QuenMatKhauRepository repository, RefreshTokenGenerator tokens, NhatKyService nhatKy,
			ApplicationEventPublisher events, AppProperties properties) {
		this.repository = repository;
		this.tokens = tokens;
		this.nhatKy = nhatKy;
		this.events = events;
		this.thoiHan = properties.security().passwordResetTtl();
	}

	@Transactional
	public KetQuaYeuCauDatLai ghi(TaiKhoanNhanEmail tk, Instant now) {
		String trangThai = this.repository.khoaTaiKhoan(tk.maNguoiDung()).orElse(null);
		if (!TrangThaiTaiKhoan.HOAT_DONG.equals(trangThai)) {
			return KetQuaYeuCauDatLai.KHONG_HOAT_DONG;
		}
		Instant dauCuaSo = now.minus(QuenMatKhauService.CUA_SO);
		if (this.repository.demToanBang(dauCuaSo) >= QuenMatKhauService.TRAN_CHUNG_MOI_GIO) {
			return KetQuaYeuCauDatLai.GIOI_HAN_CHUNG;
		}
		ThongKeYeuCau thongKe = this.repository.thongKe(tk.maNguoiDung(), dauCuaSo);
		if (thongKe.soYeuCau() >= QuenMatKhauService.TOI_DA_MOI_GIO || (thongKe.ganNhat() != null
				&& thongKe.ganNhat().isAfter(now.minus(QuenMatKhauService.KHOANG_CACH)))) {
			return KetQuaYeuCauDatLai.GIOI_HAN;
		}
		// Dòng mở cũ (kể cả đã hết hạn) vẫn giữ chỗ trong unique index một phần
		this.repository.huyConMo(tk.maNguoiDung(), now);
		String token = this.tokens.moi();
		Instant hetHan = now.plus(this.thoiHan);
		long maYeuCau = this.repository.them(tk.maNguoiDung(), RefreshTokenGenerator.sha256Hex(token), now, hetHan);
		// Đang trong transaction: dòng nhật ký chờ tới sau commit (spec 0007)
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_YEU_CAU_DAT_LAI_MAT_KHAU, null,
				DoiTuongTacDong.nguoiDung(tk.maNguoiDung()), null));
		this.events
			.publishEvent(new YeuCauDatLaiDaTao(maYeuCau, tk.maNguoiDung(), tk.email(), tk.hoTen(), token, hetHan));
		return KetQuaYeuCauDatLai.DA_TAO;
	}

}
