package vn.edu.hust.chungcu.nhatky.service;

/**
 * Thông tin của request đang chạy cần cho một dòng nhật ký. Cả hai trả {@code null} khi
 * không có request (ví dụ trong job).
 */
public interface NguCanhYeuCau {

	String requestId();

	String diaChiIp();

}
