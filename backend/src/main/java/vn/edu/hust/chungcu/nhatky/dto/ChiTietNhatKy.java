package vn.edu.hust.chungcu.nhatky.dto;

/**
 * Đánh dấu chi tiết của một sự kiện nhật ký. Mỗi sự kiện có một record riêng ở phân hệ
 * phát ra nó; không nhận {@code Map} hay chuỗi tự do, để tên khóa kiểm được lúc build
 * (spec 0007, AC-8). Không đặt thành phần nào chứa mật khẩu, token hay hash.
 */
public interface ChiTietNhatKy {

}
