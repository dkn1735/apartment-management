package vn.edu.hust.chungcu.phuongtien.controller;

import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_LY;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.BAN_QUAN_TRI;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.CU_DAN;
import static vn.edu.hust.chungcu.auth.dto.VaiTro.KE_TOAN;

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
import vn.edu.hust.chungcu.phuongtien.dto.PhuongTienCanHoResponse;
import vn.edu.hust.chungcu.phuongtien.service.TraCuuPhuongTienService;

@Tag(name = "Phương tiện")
@RestController
@RequestMapping("/api/v1/apartments")
public class PhuongTienCanHoController {

	private final TraCuuPhuongTienService traCuu;

	private final PhamViTruyCapService phamViTruyCap;

	public PhuongTienCanHoController(TraCuuPhuongTienService traCuu, PhamViTruyCapService phamViTruyCap) {
		this.traCuu = traCuu;
		this.phamViTruyCap = phamViTruyCap;
	}

	@Operation(summary = "Danh sách phương tiện của căn hộ (API-UC02-05)",
			description = "BQL, BQT và kế toán của tòa thấy đủ. Cư dân đang ở căn thấy mọi xe đang gửi nhưng đơn và lịch "
					+ "sử chỉ của xe liên quan tới mình, không kèm người cập nhật. Ngoài phạm vi trả 404.")
	@ChoPhep({ BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN, CU_DAN })
	@GetMapping("/{apartmentId}/vehicles")
	public ResponseEntity<ApiResponse<PhuongTienCanHoResponse>> xem(@PathVariable String apartmentId) {
		return ApiResponse.ok(this.traCuu.xem(apartmentId, this.phamViTruyCap.cuaNguoiDangGoi()),
				"Danh sách phương tiện của căn hộ thành công");
	}

}
