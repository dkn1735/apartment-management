package vn.edu.hust.chungcu.auth.dto;

/**
 * Mã vai trò cố định (spec 0004, mục Mã vai trò). Thứ tự khai báo là thứ tự trong claim
 * {@code vaiTro} và trong API. Tính năng 7 dùng làm authority {@code ROLE_<mã>}; nhãn
 * tiếng Việt chỉ nằm ở frontend.
 */
public enum VaiTro {

	QUAN_TRI_VIEN, BAN_QUAN_LY, BAN_QUAN_TRI, KE_TOAN, KY_THUAT, BAO_VE, CU_DAN

}
