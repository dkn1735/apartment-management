package vn.edu.hust.chungcu.auth.phanquyen;

import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.hust.chungcu.auth.dto.VaiTro;
import vn.edu.hust.chungcu.common.api.ApiResponse;

/**
 * Endpoint chỉ dành cho {@code CU_DAN}, chỉ có trong test (spec 0006, AC-3).
 * {@code @TestComponent} giữ nó ngoài component scan; test cần nó thì {@code @Import}.
 */
@TestComponent
@RestController
public class ChiCuDanController {

	public static final String PATH = "/api/v1/test/chi-cu-dan";

	@ChoPhep(VaiTro.CU_DAN)
	@GetMapping(PATH)
	public ResponseEntity<ApiResponse<String>> chiCuDan() {
		return ApiResponse.ok("ok", "Thành công");
	}

}
