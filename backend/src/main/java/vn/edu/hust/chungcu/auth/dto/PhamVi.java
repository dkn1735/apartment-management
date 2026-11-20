package vn.edu.hust.chungcu.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Phạm vi của người dùng (spec 0004, AC-8).
 *
 * @param maToaNha mọi tòa nhà khi có một trong năm vai trò nhân viên (giả định một ban
 * quản lý cho cả khu; spec 0006 AC-6 bỏ {@code QUAN_TRI_VIEN}), ngược lại rỗng
 * @param maCanHo căn hộ đang cư trú hôm nay theo giờ Việt Nam khi có vai trò
 * {@code CU_DAN}, ngược lại rỗng
 */
public record PhamVi(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> maToaNha,
		@Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<String> maCanHo) {
}
