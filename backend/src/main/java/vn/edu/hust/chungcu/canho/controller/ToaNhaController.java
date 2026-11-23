package vn.edu.hust.chungcu.canho.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_TRI;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.KE_TOAN;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.KY_THUAT;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.service.PhamViTruyCapService;
import vn.edu.hust.chungcu.canho.dto.ThemToaNhaRequest;
import vn.edu.hust.chungcu.canho.dto.ToaNhaResponse;
import vn.edu.hust.chungcu.canho.service.ThemToaNhaService;
import vn.edu.hust.chungcu.canho.service.ToaNhaService;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

@Tag(name = "Căn hộ")
@RestController
@RequestMapping("/api/v1/buildings")
public class ToaNhaController {

	private final ToaNhaService toaNhaService;

	private final ThemToaNhaService themToaNha;

	private final PhamViTruyCapService phamViTruyCap;

	public ToaNhaController(ToaNhaService toaNhaService, ThemToaNhaService themToaNha,
			PhamViTruyCapService phamViTruyCap) {
		this.toaNhaService = toaNhaService;
		this.themToaNha = themToaNha;
		this.phamViTruyCap = phamViTruyCap;
	}

	@Operation(summary = "Danh mục cấu trúc tòa nhà (API-UC01-01)",
			description = "Chỉ trả tòa nằm trong phạm vi của người gọi, kèm số căn hộ của mỗi tòa.")
	@ChoPhep({ BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN, KY_THUAT })
	@GetMapping
	public ResponseEntity<ApiResponse<PageResponse<ToaNhaResponse>>> danhSach(
			@Parameter(description = "Trang, bắt đầu từ 1",
					schema = @Schema(type = "integer", minimum = "1", defaultValue = "1")) @RequestParam(
							required = false) String page,
			@Parameter(description = "Số dòng mỗi trang, tối đa 100",
					schema = @Schema(type = "integer", minimum = "1", maximum = "100",
							defaultValue = "20")) @RequestParam(required = false) String size,
			@Parameter(description = "maToaNha, tenToaNha hoặc soTang, cộng ,asc hoặc ,desc",
					schema = @Schema(type = "string", defaultValue = ToaNhaService.SAP_XEP_MAC_DINH)) @RequestParam(
							required = false) String sort) {
		// Tham số (400) trước phạm vi (spec 0006, thứ tự kiểm)
		TrangYeuCau trang = TrangYeuCau.tu(page, size, sort, ToaNhaService.TRUONG_SAP_XEP,
				ToaNhaService.SAP_XEP_MAC_DINH);
		PhamViTruyCap phamVi = this.phamViTruyCap.cuaNguoiDangGoi();
		return ApiResponse.ok(this.toaNhaService.danhSach(phamVi.maToaNha(), trang),
				"Lấy danh mục cấu trúc tòa nhà thành công");
	}

	@Operation(summary = "Thêm tòa nhà (API-UC01-02)",
			description = "Tạo tòa cùng đủ tầng 1 đến soTang trong một transaction. Mã hoặc tên trùng (không phân biệt hoa thường) trả 409.")
	@ChoPhep(BAN_QUAN_LY)
	@PostMapping
	public ResponseEntity<ApiResponse<ToaNhaResponse>> them(@RequestBody ThemToaNhaRequest request) {
		PhamViTruyCap phamVi = this.phamViTruyCap.cuaNguoiDangGoi();
		return ApiResponse.created(this.themToaNha.them(phamVi.maNguoiDung(), request), "Thêm mới tòa nhà thành công");
	}

	@Operation(summary = "Thông tin một tòa nhà",
			description = "Ngoài danh mục 81 API (spec 0011). Tòa ngoài phạm vi hoặc không tồn tại trả 404.")
	@ChoPhep({ BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN, KY_THUAT })
	@GetMapping("/{buildingId}")
	public ResponseEntity<ApiResponse<ToaNhaResponse>> chiTiet(@PathVariable String buildingId) {
		String ma = buildingId.toUpperCase(Locale.ROOT);
		this.phamViTruyCap.cuaNguoiDangGoi().yeuCauToaNha(ma);
		return ApiResponse.ok(this.toaNhaService.chiTiet(ma), "Lấy thông tin tòa nhà thành công");
	}

}
