package vn.edu.hust.chungcu.auth.repository;

import java.util.UUID;

/** Họ phiên và chủ của nó, đọc theo hash của một refresh token. */
public record HoPhienCuaToken(UUID maHoPhien, String maNguoiDung) {

}
