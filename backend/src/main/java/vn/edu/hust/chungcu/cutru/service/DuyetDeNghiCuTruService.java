package vn.edu.hust.chungcu.cutru.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.dto.ThongTinCuDan;
import vn.edu.hust.chungcu.auth.service.CuTruDinhDanhService;
import vn.edu.hust.chungcu.auth.service.TrangThaiTaiKhoan;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruDong;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * BQL duyệt nguyên trạng một đề nghị cư trú (spec 0015, AC-4, AC-13). Thứ tự: 403 dòng
 * nhân viên BQL, 404 phạm vi, khóa căn rồi hồ sơ, 409 hết chờ, 422 tài khoản ngừng hoạt
 * động, 422 đã qua ngày kết thúc, 409 chồng ngày; rồi chuyển {@code Hợp lệ}, đồng bộ căn
 * và ghi nhật ký.
 */
@Service
public class DuyetDeNghiCuTruService {

	private final CuTruDinhDanhService dinhDanh;

	private final KhoaDeNghiCuTru khoaDeNghi;

	private final HoSoCuTruQuery query;

	private final HoSoCuTruGhi ghi;

	private final KiemChongNgay chongNgay;

	private final DongBoTrangThaiCanHo dongBo;

	private final DungHoSoCuTru dungHoSo;

	private final NhatKyService nhatKy;

	private final Clock clock;

	public DuyetDeNghiCuTruService(CuTruDinhDanhService dinhDanh, KhoaDeNghiCuTru khoaDeNghi, HoSoCuTruQuery query,
			HoSoCuTruGhi ghi, KiemChongNgay chongNgay, DongBoTrangThaiCanHo dongBo, DungHoSoCuTru dungHoSo,
			NhatKyService nhatKy, Clock clock) {
		this.dinhDanh = dinhDanh;
		this.khoaDeNghi = khoaDeNghi;
		this.query = query;
		this.ghi = ghi;
		this.chongNgay = chongNgay;
		this.dongBo = dongBo;
		this.dungHoSo = dungHoSo;
		this.nhatKy = nhatKy;
		this.clock = clock;
	}

	@Transactional
	public HoSoCuTruResponse duyet(PhamViTruyCap phamVi, String recordId) {
		String maNv = this.dinhDanh.maNvbqlCua(phamVi.maNguoiDung()).orElseThrow(CuTruLoi::khongCoQuyen);
		KhoaDeNghiCuTru.DaKhoa khoa = this.khoaDeNghi.choBql(phamVi, recordId);
		HoSoCuTruDong d = khoa.dong();
		LocalDate homNay = LocalDate.now(this.clock);
		ThongTinCuDan cuDan = this.dinhDanh.chiTietCuDanTheoMa(List.of(d.maCuDan())).get(d.maCuDan());
		if (cuDan == null || TrangThaiTaiKhoan.NGUNG_HOAT_DONG.equals(cuDan.trangThaiTaiKhoan())) {
			throw CuTruLoi.taiKhoanNgungHoatDong();
		}
		if (d.ngayKetThuc() != null && d.ngayKetThuc().isBefore(homNay)) {
			throw CuTruLoi.deNghiDaQuaNgayKetThuc();
		}
		this.chongNgay.kiem(d.maCanHo(), d.maCuDan(), new HoSoCuTruGhi.GiaTri(d.vaiTroCuTru(), d.tinhTrangCuTru(),
				d.ngayBatDau(), d.ngayKetThuc(), GiaTriCuTru.HOP_LE), d.maHoSo());
		try {
			this.ghi.ketLuanDeNghi(d.maHoSo(), GiaTriCuTru.HOP_LE, maNv, null,
					Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS));
		}
		catch (DataIntegrityViolationException ex) {
			throw KiemChongNgay.dich(ex);
		}
		ChiTietCuTru.ThayDoi<String> doiCan = this.dongBo.dongBo(d.maCanHo(), khoa.trangThaiCan(), homNay);
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.CUTRU_DUYET_DE_NGHI, phamVi.maNguoiDung(),
				DoiTuongTacDong.hoSoCuTru(d.maHoSo()),
				new ChiTietCuTru.DuyetDeNghi(d.maCanHo(), d.maCuDan(), d.maCuDanDeNghi(), d.vaiTroCuTru(),
						d.tinhTrangCuTru(), d.ngayBatDau().toString(), Objects.toString(d.ngayKetThuc(), null),
						doiCan)));
		return this.dungHoSo.dung(this.query.cua(d.maHoSo()).orElseThrow());
	}

}
