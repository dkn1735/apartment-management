package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.TaiKhoanChiTietResponse;
import vn.edu.hust.chungcu.auth.repository.KhoaTamTaiKhoanRepository;
import vn.edu.hust.chungcu.auth.repository.KhoaTamTaiKhoanRepository.KhoaCu;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Người quản lý mở khóa đăng nhập sớm (spec 0009, AC-8). Chỉ đụng ba cột bộ đếm: không
 * đổi {@code phien_ban}, trạng thái tài khoản, cũng không thu hồi phiên. Tự mở cho chính
 * mình được.
 */
@Service
public class MoKhoaDangNhapService {

	private static final Logger log = LoggerFactory.getLogger(MoKhoaDangNhapService.class);

	private final TaiKhoanDichService dich;

	private final KhoaTamTaiKhoanRepository khoaTam;

	private final TaiKhoanTraCuuService traCuu;

	private final NhatKyService nhatKy;

	private final Clock clock;

	public MoKhoaDangNhapService(TaiKhoanDichService dich, KhoaTamTaiKhoanRepository khoaTam,
			TaiKhoanTraCuuService traCuu, NhatKyService nhatKy, Clock clock) {
		this.dich = dich;
		this.khoaTam = khoaTam;
		this.traCuu = traCuu;
		this.nhatKy = nhatKy;
		this.clock = clock;
	}

	@Transactional
	public TaiKhoanChiTietResponse moKhoa(NguoiQuanLy nguoi, String maNguoiDung) {
		// Sai dạng, không có, ngoài phạm vi: 404 trước câu UPDATE (spec 0008, AC-6)
		this.dich.khoa(nguoi, maNguoiDung);
		Instant now = Instant.now(this.clock);
		Instant cu = this.khoaTam.moKhoa(maNguoiDung).map(KhoaCu::khoaTamDen).orElse(null);
		// Chỉ ghi nhật ký khi lần mở có tác dụng (AC-11)
		if (cu != null && cu.isAfter(now)) {
			log.info("Mở khóa đăng nhập: maNguoiThucHien={}, maNguoiDung={}", nguoi.maNguoiDung(), maNguoiDung);
			this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_MO_KHOA_TAM_DANG_NHAP, nguoi.maNguoiDung(),
					DoiTuongTacDong.nguoiDung(maNguoiDung), new ChiTietKhoaTam(cu)));
		}
		return this.traCuu.timChiTiet(maNguoiDung);
	}

}
