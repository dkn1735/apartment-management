package vn.edu.hust.chungcu.auth.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import vn.edu.hust.chungcu.auth.repository.KetQuaGiuLuot;
import vn.edu.hust.chungcu.auth.repository.KhoaTamDinhDanhRepository;
import vn.edu.hust.chungcu.auth.repository.KhoaTamTaiKhoanRepository;

/**
 * Khóa tạm đăng nhập (spec 0009): giữ lượt trước khi so mật khẩu, đặt lại khi mật khẩu
 * khớp, dọn dòng của định danh ảo. Tài khoản có thật đếm trên
 * {@code nguoi_dung_he_thong}; định danh không có tài khoản đếm y hệt trên
 * {@code dang_nhap_sai_dinh_danh}, để phản hồi không lộ tài khoản có tồn tại hay không.
 */
@Service
public class KhoaTamDangNhapService {

	private final KhoaTamTaiKhoanRepository taiKhoan;

	private final KhoaTamDinhDanhRepository dinhDanh;

	private final Clock clock;

	public KhoaTamDangNhapService(KhoaTamTaiKhoanRepository taiKhoan, KhoaTamDinhDanhRepository dinhDanh, Clock clock) {
		this.taiKhoan = taiKhoan;
		this.dinhDanh = dinhDanh;
		this.clock = clock;
	}

	/**
	 * Giữ một lượt: có tài khoản thì trên dòng của nó, không thì trên dòng của định danh.
	 * Chạy ngoài transaction, tự commit.
	 */
	public LuotThu giuLuot(Optional<String> maNguoiDung, TaiKhoanDangNhap dinhDanh, Instant now) {
		Optional<KetQuaGiuLuot> ketQua = maNguoiDung.isPresent()
				? this.taiKhoan.giuLuot(maNguoiDung.get(), now, KhoaTamDangNhap.SO_LAN_SAI_TOI_DA,
						KhoaTamDangNhap.THOI_GIAN_KHOA)
				: this.dinhDanh.giuLuot(RefreshTokenGenerator.sha256Hex(dinhDanh.value()), now,
						KhoaTamDangNhap.SO_LAN_SAI_TOI_DA, KhoaTamDangNhap.THOI_GIAN_KHOA);
		// Không có dòng (tài khoản bị xóa giữa chừng): coi như lượt thường
		return ketQua.map((k) -> luot(k, now)).orElseGet(LuotThu.LuotThuong::new);
	}

	/** Mật khẩu khớp: bộ đếm về 0, xóa khóa (AC-4). */
	public void datLai(String maNguoiDung) {
		this.taiKhoan.datLai(maNguoiDung);
	}

	/**
	 * Định danh vừa có chủ không mang theo trạng thái của định danh ảo (AC-19). Gọi trong
	 * transaction của tạo hoặc sửa tài khoản.
	 */
	public void xoaDinhDanh(String soDienThoai, String email) {
		List<String> hash = new ArrayList<>();
		if (soDienThoai != null) {
			hash.add(RefreshTokenGenerator.sha256Hex(soDienThoai));
		}
		if (email != null) {
			hash.add(RefreshTokenGenerator.sha256Hex(email));
		}
		this.dinhDanh.xoa(hash);
	}

	/** Xóa dòng định danh ảo đã hết tác dụng (AC-12); trả số dòng đã xóa. */
	public int donDep() {
		Instant now = Instant.now(this.clock);
		return this.dinhDanh.xoaHetTacDung(now.minus(KhoaTamDangNhap.THOI_GIAN_KHOA), now);
	}

	private static LuotThu luot(KetQuaGiuLuot ketQua, Instant now) {
		if (ketQua.vuaKhoa() == null) {
			return (ketQua.khoaCu() != null) ? new LuotThu.DangKhoa(ketQua.khoaCu()) : new LuotThu.LuotThuong();
		}
		return ketQua.vuaKhoa() ? new LuotThu.LuotCuoi(now.plus(KhoaTamDangNhap.THOI_GIAN_KHOA))
				: new LuotThu.LuotThuong();
	}

}
