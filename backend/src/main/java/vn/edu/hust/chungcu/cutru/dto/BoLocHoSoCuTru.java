package vn.edu.hust.chungcu.cutru.dto;

import java.util.List;

/**
 * Bộ lọc thô của {@code GET /api/v1/residency-records} (spec 0015, AC-9, AC-10), mọi
 * trường là chuỗi như trên URL để giá trị sai thành lỗi theo tham số.
 */
public record BoLocHoSoCuTru(String maToaNha, String soTang, List<String> trangThaiHoSo, String vaiTroCuTru,
		String tinhTrangCuTru, String q) {
}
