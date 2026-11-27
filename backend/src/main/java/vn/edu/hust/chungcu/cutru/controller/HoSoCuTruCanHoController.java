package vn.edu.hust.chungcu.cutru.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.CU_DAN;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.phanquyen.ChoPhep;
import vn.edu.hust.chungcu.auth.service.PhamViTruyCapService;
import vn.edu.hust.chungcu.common.api.ApiResponse;
import vn.edu.hust.chungcu.cutru.dto.HoSoCuTruCanHoResponse;
import vn.edu.hust.chungcu.cutru.service.TraCuuHoSoCuTruService;

@Tag(name = "Cư trú")
@RestController
@RequestMapping("/api/v1/apartments")
public class HoSoCuTruCanHoController {

	private final TraCuuHoSoCuTruService traCuu;

	private final PhamViTruyCapService phamViTruyCap;

	public HoSoCuTruCanHoController(TraCuuHoSoCuTruService traCuu, PhamViTruyCapService phamViTruyCap) {
		this.traCuu = traCuu;
		this.phamViTruyCap = phamViTruyCap;
	}

	@Operation(summary = "Tra cứu hồ sơ cư trú của căn hộ (API-UC02-01)",
			description = "BQL của tòa thấy đủ. Cư dân đang ở căn thấy người đang cư trú, hồ sơ sắp bắt đầu và lịch sử "
					+ "của riêng mình, không kèm số CCCD, điện thoại, email hay người xác minh. Ngoài phạm vi trả 404.")
	@ChoPhep({ BAN_QUAN_LY, CU_DAN })
	@GetMapping("/{apartmentId}/residents")
	public ResponseEntity<ApiResponse<HoSoCuTruCanHoResponse>> xem(@PathVariable String apartmentId) {
		return ApiResponse.ok(this.traCuu.xem(apartmentId, this.phamViTruyCap.cuaNguoiDangGoi()),
				"Tra cứu hồ sơ cư trú của căn hộ thành công");
	}

}
