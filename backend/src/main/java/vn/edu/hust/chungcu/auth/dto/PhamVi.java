package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Phạm vi của người dùng (spec 0004, AC-8).
 *
 * @param maToaNha mọi tòa nhà khi có vai trò khác {@code CU_DAN} (giả định một ban quản
 * lý cho cả khu), ngược lại rỗng
 * @param maCanHo căn hộ đang cư trú hôm nay theo giờ Việt Nam khi có vai trò
 * {@code CU_DAN}, ngược lại rỗng
 */
public record PhamVi(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> maToaNha,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> maCanHo) {
}
