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
import vn.edu.hust.chungcu.canho.service.CanHoCuTruService;
import vn.edu.hust.chungcu.common.error.LoiCoSoDuLieu;
import vn.edu.hust.chungcu.cutru.dto.GuiDeNghiCuTruRequest;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruGhi;
import vn.edu.hust.chungcu.cutru.repository.HoSoCuTruQuery;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Cư dân gửi đề nghị cư trú (spec 0015, AC-1 đến AC-3, AC-11, AC-13). Thứ tự: 403 dòng cư
 * dân, 400 body, 404 phạm vi căn, khóa dòng căn, 422 định danh, 409 đề nghị trùng, 422
 * trần đề nghị, 409 chồng ngày; rồi chèn và ghi nhật ký. Không đổi trạng thái căn.
 */
@Service
public class GuiDeNghiCuTruService {

	static final String DE_NGHI_TRUNG = "uq_hsct_cho_xac_minh_canho_cudan";

	private final CuTruDinhDanhService dinhDanh;

	private final CanHoCuTruService canHo;

	private final HoSoCuTruQuery query;

	private final HoSoCuTruGhi ghi;

	private final DungHoSoCuTru dungHoSo;

	private final NhatKyService nhatKy;

	private final Clock clock;

	public GuiDeNghiCuTruService(CuTruDinhDanhService dinhDanh, CanHoCuTruService canHo, HoSoCuTruQuery query,
			HoSoCuTruGhi ghi, DungHoSoCuTru dungHoSo, NhatKyService nhatKy, Clock clock) {
		this.dinhDanh = dinhDanh;
		this.canHo = canHo;
		this.query = query;
		this.ghi = ghi;
		this.dungHoSo = dungHoSo;
		this.nhatKy = nhatKy;
		this.clock = clock;
	}

	@Transactional
	public HoSoCuTruResponse gui(PhamViTruyCap phamVi, GuiDeNghiCuTruRequest request) {
		String nguoiGui = this.dinhDanh.maCuDanCua(phamVi.maNguoiDung()).orElseThrow(CuTruLoi::khongCoQuyen);
		LocalDate homNay = LocalDate.now(this.clock);
		NhapLieuDeNghi.DeNghiMoi moi = NhapLieuDeNghi.kiem(request, homNay);
		String ma = moi.maCanHo();
		if (!phamVi.maCanHo().contains(ma)) {
			throw CuTruLoi.khongThay();
		}
		this.canHo.khoaVaDocTrangThai(ma).orElseThrow(CuTruLoi::khongThay);
		String maCuDan = nguoiDuocDeNghi(moi, nguoiGui);
		if (this.query.coDeNghiChoXacMinh(ma, maCuDan)) {
			throw CuTruLoi.daCoDeNghiChoXacMinh();
		}
		if (this.query.demDeNghiChoXacMinh(ma) >= GiaTriCuTru.DE_NGHI_CHO_TOI_DA) {
			throw CuTruLoi.vuotSoDeNghiChoXacMinh();
		}
		HoSoCuTruGhi.GiaTri g = moi.giaTri();
		if (this.query.coChongNgayCuDan(ma, maCuDan, g.ngayBatDau(), g.ngayKetThuc(), null)) {
			throw CuTruLoi.trungKhoangNgayCuDan();
		}
		Instant luc = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		String maHoSo;
		try {
			maHoSo = this.ghi.themDeNghi(ma, maCuDan, nguoiGui, g, moi.ghiChu(), luc);
		}
		catch (DataIntegrityViolationException ex) {
			throw dich(ex);
		}
		this.nhatKy.ghi(
				new SuKienNhatKy(LoaiThaoTac.CUTRU_GUI_DE_NGHI, phamVi.maNguoiDung(), DoiTuongTacDong.hoSoCuTru(maHoSo),
						new ChiTietCuTru.GuiDeNghi(ma, maCuDan, nguoiGui, g.vaiTroCuTru(), g.tinhTrangCuTru(),
								g.ngayBatDau().toString(), Objects.toString(g.ngayKetThuc(), null),
								moi.ghiChu() != null)));
		return this.dungHoSo
			.dung(List.of(this.query.cua(maHoSo).orElseThrow()), DungHoSoCuTru.CheDo.CU_DAN_DE_NGHI, nguoiGui)
			.get(0);
	}

	/**
	 * Chính người gửi, hoặc cư dân khớp cả CCCD và số điện thoại (AC-3 bước 3). Mọi lý do
	 * không khớp khi đề nghị cho người khác đều chung một mã, để không dò được tài khoản.
	 */
	private String nguoiDuocDeNghi(NhapLieuDeNghi.DeNghiMoi moi, String nguoiGui) {
		if (moi.laChinhToi()) {
			ThongTinCuDan toi = this.dinhDanh.chiTietCuDanTheoMa(List.of(nguoiGui)).get(nguoiGui);
			if (toi == null || TrangThaiTaiKhoan.NGUNG_HOAT_DONG.equals(toi.trangThaiTaiKhoan())) {
				throw CuTruLoi.taiKhoanNgungHoatDong();
			}
			return nguoiGui;
		}
		return this.dinhDanh.timTheoCccdVaSdt(moi.soCccd(), moi.soDienThoai())
			.filter(c -> !TrangThaiTaiKhoan.NGUNG_HOAT_DONG.equals(c.trangThaiTaiKhoan()))
			.map(ThongTinCuDan::maCuDan)
			.orElseThrow(CuTruLoi::khongKhopCuDan);
	}

	/**
	 * Hai request cùng lọt qua bước kiểm đề nghị trùng thì unique index một phần chặn;
	 * ràng buộc loại trừ chồng ngày cũng đổi về 409 như {@link KiemChongNgay#dich}.
	 */
	static RuntimeException dich(DataIntegrityViolationException ex) {
		boolean trungKhoa = LoiCoSoDuLieu.sqlState(ex).filter(LoiCoSoDuLieu.TRUNG_KHOA::equals).isPresent();
		if (trungKhoa && LoiCoSoDuLieu.tenRangBuoc(ex).filter(DE_NGHI_TRUNG::equals).isPresent()) {
			return CuTruLoi.daCoDeNghiChoXacMinh();
		}
		return KiemChongNgay.dich(ex);
	}

}
