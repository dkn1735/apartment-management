package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.hust.chungcu.auth.dto.TaiKhoanChiTietResponse;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.dto.VaiTroCapNhatRequest;
import vn.edu.hust.chungcu.auth.dto.VaiTroTaiKhoan;
import vn.edu.hust.chungcu.auth.repository.BangVaiTro;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanGhi;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanQuery;
import vn.edu.hust.chungcu.auth.repository.TruongVaiTro;
import vn.edu.hust.chungcu.auth.repository.VaiTroGhi;
import vn.edu.hust.chungcu.common.error.ApiException;
import vn.edu.hust.chungcu.common.error.LoiCoSoDuLieu;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;
import vn.edu.hust.chungcu.nhatky.dto.DoiTuongTacDong;
import vn.edu.hust.chungcu.nhatky.dto.LoaiThaoTac;
import vn.edu.hust.chungcu.nhatky.dto.SuKienNhatKy;
import vn.edu.hust.chungcu.nhatky.service.NhatKyService;

/**
 * Thêm, sửa và gỡ một vai trò của tài khoản (spec 0008, AC-8, AC-9). Vai trò mới hay vai
 * trò bị gỡ có hiệu lực ở lần làm mới phiên kế tiếp; không thu hồi phiên.
 */
@Service
public class VaiTroTaiKhoanService {

	private static final Logger log = LoggerFactory.getLogger(VaiTroTaiKhoanService.class);

	private final TaiKhoanDichService dich;

	private final TaiKhoanPhamViService phamVi;

	private final TaiKhoanQuery query;

	private final TaiKhoanGhi ghi;

	private final TaiKhoanTraCuuService traCuu;

	private final NhatKyService nhatKy;

	private final Clock clock;

	public VaiTroTaiKhoanService(TaiKhoanDichService dich, TaiKhoanPhamViService phamVi, TaiKhoanQuery query,
			TaiKhoanGhi ghi, TaiKhoanTraCuuService traCuu, NhatKyService nhatKy, Clock clock) {
		this.dich = dich;
		this.phamVi = phamVi;
		this.query = query;
		this.ghi = ghi;
		this.traCuu = traCuu;
		this.nhatKy = nhatKy;
		this.clock = clock;
	}

	/** Thêm vai trò chưa có, hoặc sửa trường của vai trò đã có (AC-8). */
	@Transactional
	public TaiKhoanChiTietResponse dat(NguoiQuanLy nguoi, String maNguoiDung, String maVaiTro,
			VaiTroCapNhatRequest request) {
		List<FieldError> loi = new ArrayList<>();
		VaiTro vaiTro = vaiTroTrenPath(maVaiTro, loi);
		TaiKhoanHopLe.kiemPhienBan(request.phienBan(), loi);
		Map<TruongVaiTro, String> giaTri = (vaiTro != null) ? TaiKhoanHopLe.truongVaiTro(vaiTro, request, "", loi)
				: Map.of();
		if (!loi.isEmpty()) {
			throw TaiKhoanLoi.duLieuKhongHopLe(loi);
		}
		TaiKhoanDichService.TaiKhoanDich tk = this.dich.khoa(nguoi, maNguoiDung);
		this.phamVi.kiemSuaVaiTro(nguoi, vaiTro);
		if (tk.dong().phienBan() != request.phienBan()) {
			throw TaiKhoanLoi.lechPhienBan();
		}
		VaiTroGhi moi = new VaiTroGhi(vaiTro, giaTri);
		Optional<VaiTroTaiKhoan> cu = tk.vaiTro().stream().filter((v) -> v.vaiTro() == vaiTro).findFirst();
		List<String> doi = cu.map((c) -> truongDoi(c, moi)).orElse(List.of());
		if (cu.isPresent() && doi.isEmpty()) {
			return this.traCuu.timChiTiet(maNguoiDung);
		}
		if (vaiTro == VaiTro.CU_DAN && (cu.isEmpty() || doi.contains("soCccd"))) {
			List<String> trung = this.query.truongTrung(null, null, giaTri.get(TruongVaiTro.SO_CCCD), maNguoiDung);
			if (!trung.isEmpty()) {
				throw TaiKhoanLoi.trung(trung);
			}
		}
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		try {
			if (cu.isPresent()) {
				this.ghi.suaVaiTro(maNguoiDung, moi);
			}
			else {
				this.ghi.themVaiTro(maNguoiDung, moi);
			}
		}
		catch (DataIntegrityViolationException ex) {
			throw TaiKhoanLoi.tuRangBuoc(ex);
		}
		this.ghi.tangPhienBan(maNguoiDung, request.phienBan(), now);
		LoaiThaoTac loai = cu.isPresent() ? LoaiThaoTac.AUTH_CAP_NHAT_VAI_TRO : LoaiThaoTac.AUTH_GAN_VAI_TRO;
		ChiTietNhatKy chiTiet = cu.isPresent() ? new ChiTietTaiKhoan.CapNhatVaiTro(vaiTro, doi)
				: new ChiTietTaiKhoan.VaiTroDon(vaiTro);
		log.info("{}: maNguoiThucHien={}, maNguoiDung={}, vaiTro={}", loai, nguoi.maNguoiDung(), maNguoiDung, vaiTro);
		this.nhatKy.ghi(new SuKienNhatKy(loai, nguoi.maNguoiDung(), DoiTuongTacDong.nguoiDung(maNguoiDung), chiTiet));
		return this.traCuu.timChiTiet(maNguoiDung);
	}

	/** Gỡ một vai trò; chỉ Admin (AC-9, AC-15). */
	@Transactional
	public TaiKhoanChiTietResponse go(NguoiQuanLy nguoi, String maNguoiDung, String maVaiTro, String phienBanRaw) {
		List<FieldError> loi = new ArrayList<>();
		VaiTro vaiTro = vaiTroTrenPath(maVaiTro, loi);
		Long phienBan = TaiKhoanHopLe.docPhienBan(phienBanRaw, loi);
		if (!loi.isEmpty()) {
			throw TaiKhoanLoi.duLieuKhongHopLe(loi);
		}
		// Gỡ quyền quản trị có thể làm giảm số Admin đang hoạt động: khóa trước khi đọc
		// tài
		// khoản đích (AC-11)
		if (vaiTro == VaiTro.QUAN_TRI_VIEN) {
			this.ghi.khoaAdmin();
		}
		TaiKhoanDichService.TaiKhoanDich tk = this.dich.khoa(nguoi, maNguoiDung);
		this.phamVi.kiemGoVaiTro(nguoi);
		if (!tk.co(vaiTro)) {
			throw ApiException.notFound("Tài khoản không có vai trò này");
		}
		if (tk.dong().phienBan() != phienBan) {
			throw TaiKhoanLoi.lechPhienBan();
		}
		if (tk.vaiTro().size() == 1) {
			throw new ApiException(HttpStatus.UNPROCESSABLE_CONTENT, AuthErrorCodes.AUTH_LAST_ROLE,
					"Tài khoản phải có ít nhất một vai trò. Nếu người này không còn dùng hệ thống, hãy chuyển sang Ngừng hoạt động.");
		}
		if (vaiTro == VaiTro.QUAN_TRI_VIEN) {
			if (maNguoiDung.equals(nguoi.maNguoiDung())) {
				throw TaiKhoanLoi.tuBaoVe();
			}
			if (tk.dangHoatDong() && this.query.demAdminHoatDongTru(maNguoiDung) < 1) {
				throw TaiKhoanLoi.adminCuoi();
			}
		}
		try {
			this.ghi.xoaVaiTro(maNguoiDung, BangVaiTro.cua(vaiTro));
		}
		catch (DataIntegrityViolationException ex) {
			String sqlState = LoiCoSoDuLieu.sqlState(ex).orElse("");
			if (LoiCoSoDuLieu.KHOA_NGOAI.equals(sqlState) || LoiCoSoDuLieu.RANG_BUOC_RESTRICT.equals(sqlState)) {
				throw new ApiException(HttpStatus.CONFLICT, AuthErrorCodes.AUTH_ROLE_IN_USE,
						"Vai trò này đang gắn với dữ liệu khác nên không gỡ được. Nếu người này không còn làm việc, hãy chuyển tài khoản sang Ngừng hoạt động.");
			}
			throw ex;
		}
		this.ghi.tangPhienBan(maNguoiDung, phienBan, Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS));
		log.info("Gỡ vai trò: maNguoiThucHien={}, maNguoiDung={}, vaiTro={}", nguoi.maNguoiDung(), maNguoiDung, vaiTro);
		this.nhatKy.ghi(new SuKienNhatKy(LoaiThaoTac.AUTH_GO_VAI_TRO, nguoi.maNguoiDung(),
				DoiTuongTacDong.nguoiDung(maNguoiDung), new ChiTietTaiKhoan.VaiTroDon(vaiTro)));
		return this.traCuu.timChiTiet(maNguoiDung);
	}

	private static VaiTro vaiTroTrenPath(String ma, List<FieldError> loi) {
		VaiTro vaiTro = TaiKhoanHopLe.docVaiTro(ma);
		if (vaiTro == null) {
			loi.add(new FieldError("vaiTro", "Vai trò không tồn tại"));
		}
		return vaiTro;
	}

	/** Tên các trường có giá trị khác giữa dòng đang có và giá trị mới. */
	private static List<String> truongDoi(VaiTroTaiKhoan cu, VaiTroGhi moi) {
		List<String> doi = new ArrayList<>();
		for (TruongVaiTro truong : BangVaiTro.cua(moi.vaiTro()).truong()) {
			if (!Objects.equals(giaTriCu(cu, truong), moi.giaTri().get(truong))) {
				doi.add(truong.ten());
			}
		}
		return doi;
	}

	private static String giaTriCu(VaiTroTaiKhoan cu, TruongVaiTro truong) {
		return switch (truong) {
			case CHUC_VU -> cu.chucVu();
			case BO_PHAN -> cu.boPhan();
			case NHIEM_KY -> cu.nhiemKy();
			case VAI_TRO_BQT -> cu.vaiTroBqt();
			case PHAM_VI_PHU_TRACH -> cu.phamViPhuTrach();
			case CHUYEN_MON_KY_THUAT -> cu.chuyenMonKyThuat();
			case VI_TRI_TRUC -> cu.viTriTruc();
			case SO_CCCD -> cu.soCccd();
		};
	}

}
