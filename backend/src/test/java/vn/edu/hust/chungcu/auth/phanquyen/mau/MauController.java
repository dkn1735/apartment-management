package vn.edu.hust.chungcu.auth.phanquyen.mau;

import org.springframework.security.access.prepost.PreAuthorize;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.phanquyen.CongKhai;
import vn.edu.hust.chungcu.auth.phanquyen.DaDangNhap;

/**
 * Controller mẫu cố tình khai sai cho {@code PhanQuyenCoverageMauTest}. Không mang
 * {@code @RestController}, nên không bao giờ được đăng ký vào app; test tự dựng
 * {@code HandlerMethod} từ các method này.
 */
public class MauController {

	public void thieu() {
	}

	@ChoPhep(VaiTro.BAN_QUAN_LY)
	@DaDangNhap
	public void haiAnnotation() {
	}

	@ChoPhep({})
	public void choPhepRong() {
	}

	@PreAuthorize("hasRole('BAN_QUAN_LY')")
	@ChoPhep(VaiTro.BAN_QUAN_LY)
	public void preAuthorize() {
	}

	@CongKhai
	public void congKhai() {
	}

	@ChoPhep({ VaiTro.KE_TOAN, VaiTro.BAN_QUAN_LY })
	public void dung() {
	}

}
