package vn.edu.hust.chungcu.phuongtien.service;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.CU_DAN;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;
import vn.edu.hust.chungcu.phuongtien.dto.DangKyPhuongTienRequest;
import vn.edu.hust.chungcu.phuongtien.dto.PhuongTienResponse;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienGhi;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienQuery;

/**
 * Đăng ký phương tiện (spec 0016, AC-5 đến AC-8, AC-12). BQL đăng ký thì xe hoạt động
 * ngay; cư dân gửi thì thành đơn chờ duyệt. Thứ tự: 403, 400, 404, khóa dòng căn, 422 chủ
 * xe, 422 trần đơn chờ, 409 biển số. Lỗi ràng buộc được đổi nghĩa bên ngoài transaction.
 */
@Service
public class DangKyPhuongTienService {

	static final int DON_CHO_TOI_DA = 10;

	private final CuTruDinhDanhService dinhDanh;

	private final CanHoCuTruService canHo;

	private final KiemChuXe kiemChuXe;

	private final KiemTrungBien kiemTrungBien;

	private final PhuongTienQuery query;

	private final PhuongTienGhi ghi;

	private final HienThiPhuongTien hienThi;

	private final NhatKyService nhatKy;

	private final Clock clock;

	private final TransactionTemplate giaoDich;

	DangKyPhuongTienService(CuTruDinhDanhService dinhDanh, CanHoCuTruService canHo, KiemChuXe kiemChuXe,
			KiemTrungBien kiemTrungBien, PhuongTienQuery query, PhuongTienGhi ghi, HienThiPhuongTien hienThi,
			NhatKyService nhatKy, Clock clock, PlatformTransactionManager transactionManager) {
		this.dinhDanh = dinhDanh;
		this.canHo = canHo;
		this.kiemChuXe = kiemChuXe;
		this.kiemTrungBien = kiemTrungBien;
		this.query = query;
		this.ghi = ghi;
		this.hienThi = hienThi;
		this.nhatKy = nhatKy;
		this.clock = clock;
		this.giaoDich = new TransactionTemplate(transactionManager);
	}

	public PhuongTienResponse dangKy(PhamViTruyCap phamVi, DangKyPhuongTienRequest request) {
		try {
			return this.giaoDich.execute(status -> trongGiaoDich(phamVi, request));
		}
		catch (DataIntegrityViolationException ex) {
			throw PhuongTienLoi.dich(ex);
		}
	}

	private PhuongTienResponse trongGiaoDich(PhamViTruyCap phamVi, DangKyPhuongTienRequest request) {
		boolean luongBql = phamVi.coVaiTro(BAN_QUAN_LY);
		String maNguoiDung = phamVi.maNguoiDung();
		String maNv = luongBql ? this.dinhDanh.maNvbqlCua(maNguoiDung).orElseThrow(PhuongTienLoi::khongCoQuyen) : null;
		String toi = phamVi.coVaiTro(CU_DAN) ? this.dinhDanh.maCuDanCua(maNguoiDung).orElse(null) : null;
		if (!luongBql && toi == null) {
			throw PhuongTienLoi.khongCoQuyen();
		}
		NhapLieuPhuongTien.DangKy moi = NhapLieuPhuongTien.dangKy(request);
		String maCanHo = moi.maCanHo();
		String maToaNha = this.canHo.maToaNhaCua(maCanHo).orElseThrow(PhuongTienLoi::khongThay);
		boolean trongPhamVi = luongBql ? phamVi.maToaNha().contains(maToaNha) : phamVi.maCanHo().contains(maCanHo);
		if (!trongPhamVi) {
			throw PhuongTienLoi.khongThay();
		}
		this.canHo.khoaVaDocTrangThai(maCanHo).orElseThrow(PhuongTienLoi::khongThay);
		this.kiemChuXe.kiem(maCanHo, moi.maCuDanSoHuu(), LocalDate.now(this.clock), true);
		if (!luongBql && this.query.demChoDuyet(maCanHo) >= DON_CHO_TOI_DA) {
			throw PhuongTienLoi.vuotSoDonChoDuyet();
		}
		this.kiemTrungBien.kiem(moi.bienSo(), phamVi, luongBql);
		String trangThai = luongBql ? GiaTriPhuongTien.DANG_HOAT_DONG : GiaTriPhuongTien.CHO_DUYET;
		String ma = this.ghi.them(
				new PhuongTienGhi.Moi(maCanHo, moi.maCuDanSoHuu(), moi.loaiPhuongTien(), moi.bienSo(),
						moi.nhanHieuMauSac(), trangThai, luongBql ? null : toi, maNv),
				Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS));
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.PHUONGTIEN_DANG_KY, maNguoiDung, DoiTuongTacDong.phuongTien(ma),
				new ChiTietPhuongTien(maCanHo, moi.maCuDanSoHuu(), moi.loaiPhuongTien(), moi.bienSo(),
						new ChiTietPhuongTien.TrangThai(null, trangThai), null)));
		GocNhin gocNhin = GocNhin.cua(phamVi, maToaNha, maCanHo).orElseThrow(PhuongTienLoi::khongThay);
		return this.hienThi.dung(maCanHo, List.of(this.query.cua(ma).orElseThrow()), gocNhin, toi).get(0);
	}

}
