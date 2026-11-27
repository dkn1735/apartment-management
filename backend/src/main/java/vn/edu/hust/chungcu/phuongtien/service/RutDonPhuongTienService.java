package vn.edu.hust.chungcu.phuongtien.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;
import vn.edu.hust.chungcu.phuongtien.dto.PhuongTienResponse;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienDong;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienGhi;
import vn.edu.hust.chungcu.phuongtien.repository.PhuongTienQuery;

/**
 * Người gửi tự rút đơn đang chờ duyệt (spec 0016, AC-11). Mọi trường hợp không phải đơn
 * của chính mình tại căn đang ở đều là 404 chung.
 */
@Service
public class RutDonPhuongTienService {

	private final CuTruDinhDanhService dinhDanh;

	private final CanHoCuTruService canHo;

	private final PhuongTienQuery query;

	private final PhuongTienGhi ghi;

	private final HienThiPhuongTien hienThi;

	private final NhatKyService nhatKy;

	private final Clock clock;

	RutDonPhuongTienService(CuTruDinhDanhService dinhDanh, CanHoCuTruService canHo, PhuongTienQuery query,
			PhuongTienGhi ghi, HienThiPhuongTien hienThi, NhatKyService nhatKy, Clock clock) {
		this.dinhDanh = dinhDanh;
		this.canHo = canHo;
		this.query = query;
		this.ghi = ghi;
		this.hienThi = hienThi;
		this.nhatKy = nhatKy;
		this.clock = clock;
	}

	@Transactional
	public PhuongTienResponse rut(PhamViTruyCap phamVi, String vehicleId) {
		String toi = this.dinhDanh.maCuDanCua(phamVi.maNguoiDung()).orElseThrow(PhuongTienLoi::khongCoQuyen);
		PhuongTienDong dong = khoa(phamVi, vehicleId, toi);
		if (!GiaTriPhuongTien.CHO_DUYET.equals(dong.trangThaiDangKy())) {
			throw PhuongTienLoi.donKhongConChoDuyet();
		}
		String ma = dong.maPhuongTien();
		this.ghi.doiTrangThai(ma, GiaTriPhuongTien.DA_HUY, null, null,
				Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS));
		this.nhatKy
			.ghi(new SuKienNhatKy(LoaiThaoTac.PHUONGTIEN_RUT_DON, phamVi.maNguoiDung(), DoiTuongTacDong.phuongTien(ma),
					new ChiTietPhuongTien(dong.maCanHo(), dong.maCuDanSoHuu(), dong.loaiPhuongTien(), dong.bienSo(),
							new ChiTietPhuongTien.TrangThai(dong.trangThaiDangKy(), GiaTriPhuongTien.DA_HUY), null)));
		return this.hienThi.dung(dong.maCanHo(), List.of(this.query.cua(ma).orElseThrow()), GocNhin.CU_DAN_CAN_HO, toi)
			.get(0);
	}

	private PhuongTienDong khoa(PhamViTruyCap phamVi, String vehicleId, String toi) {
		String ma = NhapLieuPhuongTien.maPhuongTien(vehicleId);
		PhuongTienDong dong = (ma != null) ? this.query.cua(ma).orElse(null) : null;
		if (dong == null || !phamVi.maCanHo().contains(dong.maCanHo()) || !toi.equals(dong.maCuDanDangKy())) {
			throw PhuongTienLoi.khongThay();
		}
		this.canHo.khoaVaDocTrangThai(dong.maCanHo()).orElseThrow(PhuongTienLoi::khongThay);
		PhuongTienDong daKhoa = this.query.khoaDong(ma).orElseThrow(PhuongTienLoi::khongThay);
		if (!toi.equals(daKhoa.maCuDanDangKy())) {
			throw PhuongTienLoi.khongThay();
		}
		return daKhoa;
	}

}
