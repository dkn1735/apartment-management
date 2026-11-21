package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.dto.TaoTaiKhoanRequest;
import vn.edu.hust.chungcu.auth.dto.TaoTaiKhoanResponse;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.repository.TaiKhoanQuery;
import vn.edu.hust.chungcu.auth.repository.TruongVaiTro;
import vn.edu.hust.chungcu.auth.repository.VaiTroGhi;
import vn.edu.hust.chungcu.common.error.ErrorResponse.FieldError;

/**
 * Tạo tài khoản (spec 0008, AC-1 đến AC-4). Kiểm, sinh mật khẩu tạm và băm bcrypt nằm
 * ngoài transaction; phần ghi ở {@link GhiTaiKhoanMoiService}.
 */
@Service
public class TaoTaiKhoanService {

	private final TaiKhoanPhamViService phamVi;

	private final TaiKhoanQuery query;

	private final MatKhauTamGenerator matKhauTam;

	private final PasswordEncoder encoder;

	private final GhiTaiKhoanMoiService ghi;

	private final TaiKhoanTraCuuService traCuu;

	private final Clock clock;

	public TaoTaiKhoanService(TaiKhoanPhamViService phamVi, TaiKhoanQuery query, MatKhauTamGenerator matKhauTam,
			PasswordEncoder encoder, GhiTaiKhoanMoiService ghi, TaiKhoanTraCuuService traCuu, Clock clock) {
		this.phamVi = phamVi;
		this.query = query;
		this.matKhauTam = matKhauTam;
		this.encoder = encoder;
		this.ghi = ghi;
		this.traCuu = traCuu;
		this.clock = clock;
	}

	public TaoTaiKhoanResponse tao(NguoiQuanLy nguoi, TaoTaiKhoanRequest request) {
		List<FieldError> loi = new ArrayList<>();
		TaiKhoanHopLe.ThongTinChung thongTin = TaiKhoanHopLe.thongTinChung(request.hoTen(), request.soDienThoai(),
				request.email(), loi);
		List<VaiTroGhi> vaiTro = TaiKhoanHopLe.danhSachVaiTro(request.vaiTro(), loi);
		if (!loi.isEmpty()) {
			throw TaiKhoanLoi.duLieuKhongHopLe(loi);
		}
		this.phamVi.kiemTao(nguoi, vaiTro.stream().map(VaiTroGhi::vaiTro).toList());
		String soCccd = vaiTro.stream()
			.filter((v) -> v.vaiTro() == VaiTro.CU_DAN)
			.map((v) -> v.giaTri().get(TruongVaiTro.SO_CCCD))
			.findFirst()
			.orElse(null);
		List<String> trung = this.query.truongTrung(thongTin.soDienThoai(), thongTin.email(), soCccd, null);
		if (!trung.isEmpty()) {
			throw TaiKhoanLoi.trung(trung);
		}
		String matKhau = this.matKhauTam.sinh();
		String hash = this.encoder.encode(matKhau);
		// PostgreSQL lưu tới micro giây; cắt trước để giá trị trả về bằng giá trị đã lưu
		Instant now = Instant.now(this.clock).truncatedTo(ChronoUnit.MICROS);
		String maNguoiDung = this.ghi.ghi(nguoi.maNguoiDung(), thongTin, vaiTro, hash, now);
		return new TaoTaiKhoanResponse(this.traCuu.timChiTiet(maNguoiDung), matKhau);
	}

}
