package vn.edu.hust.chungcu.phuongtien.service;

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
import vn.edu.hust.chungcu.phuongtien.dto.DoiTrangThaiPhuongTienRequest;
import vn.edu.hust.chungcu.phuongtien.dto.PhuongTienResponse;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienDong;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienGhi;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienQuery;

/**
 * BQL duyệt, từ chối, tạm dừng, kích hoạt lại hoặc hủy thẻ xe (spec 0016, AC-9, AC-10).
 * Khóa dòng căn trước rồi mới khóa dòng phương tiện, cùng thứ tự với cư trú.
 */
@Service
public class DoiTrangThaiPhuongTienService {

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

	DoiTrangThaiPhuongTienService(CuTruDinhDanhService dinhDanh, CanHoCuTruService canHo, KiemChuXe kiemChuXe,
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

	public PhuongTienResponse doi(PhamViTruyCap phamVi, String vehicleId, DoiTrangThaiPhuongTienRequest request) {
		try {
			return this.giaoDich.execute(status -> trongGiaoDich(phamVi, vehicleId, request));
		}
		catch (DataIntegrityViolationException ex) {
			throw PhuongTienLoi.dich(ex);
		}
	}

	private PhuongTienResponse trongGiaoDich(PhamViTruyCap phamVi, String vehicleId,
			DoiTrangThaiPhuongTienRequest request) {
		String maNv = this.dinhDanh.maNvbqlCua(phamVi.maNguoiDung()).orElseThrow(PhuongTienLoi::khongCoQuyen);
		NhapLieuPhuongTien.DoiTrangThai moi = NhapLieuPhuongTien.doiTrangThai(request);
		PhuongTienDong dong = khoa(phamVi, vehicleId);
		String cu = dong.trangThaiDangKy();
		String sang = moi.trangThai();
		if (!MaTranTrangThai.chuyenDuoc(cu, sang)) {
			throw PhuongTienLoi.chuyenTrangThaiKhongHopLe();
		}
		if (GiaTriPhuongTien.DANG_HOAT_DONG.equals(sang)) {
			this.kiemChuXe.kiem(dong.maCanHo(), dong.maCuDanSoHuu(), LocalDate.now(this.clock), false);
			if (GiaTriPhuongTien.CHO_DUYET.equals(cu)) {
				this.kiemTrungBien.kiem(dong.bienSo(), phamVi, true);
			}
		}
		String ma = dong.maPhuongTien();
		this.ghi.doiTrangThai(ma, sang, moi.lyDo(), maNv, Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS));
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.PHUONGTIEN_DOI_TRANG_THAI, phamVi.maNguoiDung(),
				DoiTuongTacDong.phuongTien(ma), new ChiTietPhuongTien(dong.maCanHo(), dong.maCuDanSoHuu(),
						dong.loaiPhuongTien(), dong.bienSo(), new ChiTietPhuongTien.TrangThai(cu, sang), moi.lyDo())));
		String toi = phamVi.coVaiTro(CU_DAN) ? this.dinhDanh.maCuDanCua(phamVi.maNguoiDung()).orElse(null) : null;
		return this.hienThi.dung(dong.maCanHo(), List.of(this.query.cua(ma).orElseThrow()), GocNhin.NHAN_VIEN, toi)
			.get(0);
	}

	/** 404 khi mã sai, không có hoặc căn ngoài tòa phạm vi. */
	private PhuongTienDong khoa(PhamViTruyCap phamVi, String vehicleId) {
		String ma = NhapLieuPhuongTien.maPhuongTien(vehicleId);
		// Đọc chưa khóa chỉ để biết căn cần khóa; ma_can_ho không bao giờ đổi
		String maCanHo = (ma != null) ? this.query.cua(ma).map(PhuongTienDong::maCanHo).orElse(null) : null;
		if (maCanHo == null || this.canHo.maToaNhaCua(maCanHo).filter(phamVi.maToaNha()::contains).isEmpty()) {
			throw PhuongTienLoi.khongThay();
		}
		this.canHo.khoaVaDocTrangThai(maCanHo).orElseThrow(PhuongTienLoi::khongThay);
		return this.query.khoaDong(ma).orElseThrow(PhuongTienLoi::khongThay);
	}

}
