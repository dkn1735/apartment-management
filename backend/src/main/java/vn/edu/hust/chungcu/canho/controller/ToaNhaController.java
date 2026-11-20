package vn.edu.hust.chungcu.canho.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_TRI;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.KE_TOAN;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.dto.PhamViTruyCap;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.service.PhamViTruyCapService;
import vn.edu.hust.chungcu.canho.dto.ToaNhaResponse;
import vn.edu.hust.chungcu.canho.service.ToaNhaService;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.common.api.PageResponse;
import vn.edu.hust.chungcu.common.api.TrangYeuCau;

@Tag(name = "Căn hộ")
@RestController
@RequestMapping("/api/v1/buildings")
public class ToaNhaController {

	private final ToaNhaService toaNhaService;

	private final PhamViTruyCapService phamViTruyCap;

	public ToaNhaController(ToaNhaService toaNhaService, PhamViTruyCapService phamViTruyCap) {
		this.toaNhaService = toaNhaService;
		this.phamViTruyCap = phamViTruyCap;
	}

	@Operation(summary = "Danh mục cấu trúc tòa nhà (API-UC01-01)",
			description = "Chỉ trả tòa nằm trong phạm vi của người gọi, kèm số căn hộ của mỗi tòa.")
	@ChoPhep({ BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN })
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

}
