package vn.edu.hust.chungcu.canho.dto;

import java.util.List;

/**
 * Tham số lọc của {@code GET /api/v1/apartments} đúng như trên query, chưa kiểm (spec
 * 0012, AC-2; spec 0013, AC-11). Mọi trường có thể {@code null}.
 */
public record BoLocCanHo(String maToaNha, String soTang, String dienTichTu, String dienTichDen,
		List<String> trangThaiSuDung, String q, String biTamDungTienIch) {
}
