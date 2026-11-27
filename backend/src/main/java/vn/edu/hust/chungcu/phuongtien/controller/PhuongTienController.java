package vn.edu.hust.chungcu.phuongtien.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.CU_DAN;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.service.PhamViTruyCapService;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.phuongtien.dto.DangKyPhuongTienRequest;
import vn.edu.hust.chungcu.phuongtien.dto.DoiTrangThaiPhuongTienRequest;
import vn.edu.hust.chungcu.phuongtien.dto.PhuongTienResponse;
import vn.edu.hust.chungcu.phuongtien.service.DangKyPhuongTienService;
import vn.edu.hust.chungcu.phuongtien.service.DoiTrangThaiPhuongTienService;
import vn.edu.hust.chungcu.phuongtien.service.RutDonPhuongTienService;

/** Đăng ký và vòng đời thẻ xe (spec 0016, API surface). */
@Tag(name = "Phương tiện")
@RestController
@RequestMapping("/api/v1/vehicles")
public class PhuongTienController {

	private final DangKyPhuongTienService dangKy;

	private final DoiTrangThaiPhuongTienService doiTrangThai;

	private final RutDonPhuongTienService rutDon;

	private final PhamViTruyCapService phamViTruyCap;

	public PhuongTienController(DangKyPhuongTienService dangKy, DoiTrangThaiPhuongTienService doiTrangThai,
			RutDonPhuongTienService rutDon, PhamViTruyCapService phamViTruyCap) {
		this.dangKy = dangKy;
		this.doiTrangThai = doiTrangThai;
		this.rutDon = rutDon;
		this.phamViTruyCap = phamViTruyCap;
	}

	@Operation(summary = "Đăng ký phương tiện gửi xe (API-UC02-06)",
			description = "BQL đăng ký thì thẻ xe hoạt động ngay; cư dân gửi thì thành đơn chờ duyệt, tối đa 10 đơn mỗi "
					+ "căn. Chủ xe phải đang cư trú hợp lệ tại căn. Biển số được chuẩn hóa, chỉ xe đạp được để trống.")
	@ChoPhep({ BAN_QUAN_LY, CU_DAN })
	@PostMapping
	public ResponseEntity<ApiResponse<PhuongTienResponse>> dangKy(@RequestBody DangKyPhuongTienRequest request) {
		return ApiResponse.created(this.dangKy.dangKy(this.phamViTruyCap.cuaNguoiDangGoi(), request),
				"Đăng ký phương tiện gửi xe thành công");
	}

	@Operation(summary = "Đổi trạng thái thẻ xe (API-UC02-07)",
			description = "Duyệt, từ chối, tạm dừng, kích hoạt lại hoặc hủy. Từ chối và Đã hủy là trạng thái cuối. "
					+ "Duyệt và kích hoạt lại kiểm lại chủ xe còn cư trú và tài khoản chưa ngừng hoạt động.")
	@ChoPhep(BAN_QUAN_LY)
	@PutMapping("/{vehicleId}/status")
	public ResponseEntity<ApiResponse<PhuongTienResponse>> doiTrangThai(@PathVariable String vehicleId,
			@RequestBody DoiTrangThaiPhuongTienRequest request) {
		return ApiResponse.ok(this.doiTrangThai.doi(this.phamViTruyCap.cuaNguoiDangGoi(), vehicleId, request),
				"Cập nhật trạng thái thẻ xe/phương tiện thành công");
	}

	@Operation(summary = "Rút đơn đăng ký phương tiện của chính mình",
			description = "Người gửi rút đơn còn chờ duyệt. Đơn không phải của mình trả 404.")
	@ChoPhep(CU_DAN)
	@PostMapping("/{vehicleId}/cancel")
	public ResponseEntity<ApiResponse<PhuongTienResponse>> rut(@PathVariable String vehicleId) {
		return ApiResponse.ok(this.rutDon.rut(this.phamViTruyCap.cuaNguoiDangGoi(), vehicleId),
				"Rút đơn đăng ký phương tiện thành công");
	}

}
