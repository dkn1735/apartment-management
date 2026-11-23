/**
 * Ngưỡng và thời gian khóa tạm đăng nhập, trùng hằng `KhoaTamDangNhap` ở backend (spec 0009). Chỉ dùng cho câu gợi ý;
 * số phút khi đang khóa luôn lấy từ `Retry-After`.
 */
export const SO_LAN_SAI_TOI_DA = 5;

export const PHUT_KHOA_TAM = 15;

export const CAU_GOI_Y_KHOA_TAM = `Nhập sai ${SO_LAN_SAI_TOI_DA} lần liên tiếp sẽ bị tạm khóa đăng nhập ${PHUT_KHOA_TAM} phút.`;
