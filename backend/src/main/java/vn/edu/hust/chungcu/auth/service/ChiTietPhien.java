package vn.edu.hust.chungcu.auth.service;

import java.util.UUID;
import vn.edu.hust.chungcu.nhatky.dto.ChiTietNhatKy;

/** Chi tiết nhật ký của đăng nhập thành công, đăng xuất và dùng lại token (spec 0007). */
public record ChiTietPhien(UUID maHoPhien) implements ChiTietNhatKy {

}
