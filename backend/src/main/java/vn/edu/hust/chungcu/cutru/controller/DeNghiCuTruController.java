package vn.edu.hust.chungcu.cutru.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.CU_DAN;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.service.PhamViTruyCapService;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.cutru.dto.GuiDeNghiCuTruRequest;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruResponse;
import vn.edu.hust.chungcu.cutru.dto.TuChoiDeNghiCuTruRequest;
import vn.edu.hust.chungcu.cutru.service.DuyetDeNghiCuTruService;
import vn.edu.hust.chungcu.cutru.service.GuiDeNghiCuTruService;
import vn.edu.hust.chungcu.cutru.service.HuyDeNghiCuTruService;
import vn.edu.hust.chungcu.cutru.service.TuChoiDeNghiCuTruService;

/** Vòng đời đề nghị cư trú do cư dân gửi (spec 0015, API surface). */
@Tag(name = "Cư trú")
@RestController
@RequestMapping("/api/v1/residency-records")
public class DeNghiCuTruController {

	private final GuiDeNghiCuTruService gui;

	private final DuyetDeNghiCuTruService duyet;

	private final TuChoiDeNghiCuTruService tuChoi;

	private final HuyDeNghiCuTruService huy;

	private final PhamViTruyCapService phamViTruyCap;

	public DeNghiCuTruController(GuiDeNghiCuTruService gui, DuyetDeNghiCuTruService duyet,
			TuChoiDeNghiCuTruService tuChoi, HuyDeNghiCuTruService huy, PhamViTruyCapService phamViTruyCap) {
		this.gui = gui;
		this.duyet = duyet;
		this.tuChoi = tuChoi;
		this.huy = huy;
		this.phamViTruyCap = phamViTruyCap;
	}

	@Operation(summary = "Cư dân gửi đề nghị cư trú (API-UC02-04)",
			description = "Chỉ cho căn người gửi đang cư trú hợp lệ hôm nay. Đề nghị cho người khác phải nhập khớp cả "
					+ "số CCCD và số điện thoại của một tài khoản cư dân. Đề nghị ở trạng thái Chờ xác minh, không đổi "
					+ "trạng thái căn; phản hồi không kèm dữ liệu cá nhân.")
	@ChoPhep(CU_DAN)
	@PostMapping("/resident-request")
	public ResponseEntity<ApiResponse<HoSoCuTruResponse>> gui(@RequestBody GuiDeNghiCuTruRequest request) {
		return ApiResponse.created(this.gui.gui(this.phamViTruyCap.cuaNguoiDangGoi(), request),
				"Cư dân gửi yêu cầu cập nhật hồ sơ thành công");
	}

	@Operation(summary = "Duyệt đề nghị cư trú",
			description = "Duyệt nguyên trạng đề nghị đang chờ xác minh; hồ sơ sang Hợp lệ và căn tự đồng bộ trạng thái "
					+ "sử dụng. Đề nghị đã qua ngày kết thúc phải từ chối.")
	@ChoPhep(BAN_QUAN_LY)
	@PostMapping("/{recordId}/approve")
	public ResponseEntity<ApiResponse<HoSoCuTruResponse>> duyet(@PathVariable String recordId) {
		return ApiResponse.ok(this.duyet.duyet(this.phamViTruyCap.cuaNguoiDangGoi(), recordId),
				"Duyệt đề nghị cư trú thành công");
	}

	@Operation(summary = "Từ chối đề nghị cư trú",
			description = "Đề nghị đang chờ xác minh sang Từ chối kèm lý do; không đổi trạng thái căn.")
	@ChoPhep(BAN_QUAN_LY)
	@PostMapping("/{recordId}/reject")
	public ResponseEntity<ApiResponse<HoSoCuTruResponse>> tuChoi(@PathVariable String recordId,
			@RequestBody TuChoiDeNghiCuTruRequest request) {
		return ApiResponse.ok(this.tuChoi.tuChoi(this.phamViTruyCap.cuaNguoiDangGoi(), recordId, request),
				"Từ chối đề nghị cư trú thành công");
	}

	@Operation(summary = "Hủy đề nghị cư trú của chính mình",
			description = "Người gửi rút đề nghị còn chờ xác minh. Đề nghị không phải của mình trả 404.")
	@ChoPhep(CU_DAN)
	@PostMapping("/{recordId}/cancel")
	public ResponseEntity<ApiResponse<HoSoCuTruResponse>> huy(@PathVariable String recordId) {
		return ApiResponse.ok(this.huy.huy(this.phamViTruyCap.cuaNguoiDangGoi(), recordId),
				"Hủy đề nghị cư trú thành công");
	}

}
