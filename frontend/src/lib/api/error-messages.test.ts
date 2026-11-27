import { describe, expect, it } from "vitest";
import { ApiError } from "./api";
import { messageFor, requestIdToShow, thongBaoKhoaTam } from "./error-messages";

const error = (status: number, errorCode: string, requestId?: string) =>
  new ApiError({ status, errorCode, message: "raw", requestId });

describe("messageFor", () => {
  it("dùng câu trong từ điển theo errorCode", () => {
    expect(messageFor(error(409, "CONFLICT"))).toBe(
      "Dữ liệu bị trùng hoặc đang ở trạng thái không cho phép thao tác này.",
    );
  });

  it("mã chưa có thì dùng câu chung theo mã HTTP", () => {
    expect(messageFor(error(422, "XYZ"))).toBe("Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại.");
    expect(messageFor(error(503, "XYZ"))).toBe("Hệ thống đang gặp sự cố, vui lòng thử lại sau.");
    expect(messageFor(error(0, "toString"))).toBe("Hệ thống đang gặp sự cố, vui lòng thử lại sau.");
  });
});

describe("requestIdToShow", () => {
  it("hiện mã cho 5xx và BACKEND_UNAVAILABLE, không cho 4xx", () => {
    expect(requestIdToShow(error(500, "INTERNAL_ERROR", "abc"))).toBe("abc");
    expect(requestIdToShow(error(0, "BACKEND_UNAVAILABLE", "abc"))).toBe("abc");
    expect(requestIdToShow(error(409, "CONFLICT", "abc"))).toBeUndefined();
    expect(requestIdToShow(error(500, "INTERNAL_ERROR"))).toBeUndefined();
  });
});

describe("thông báo khóa tạm (spec 0009, AC-13)", () => {
  it.each([
    [720, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 12 phút hoặc liên hệ ban quản lý."],
    [721, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 13 phút hoặc liên hệ ban quản lý."],
    [1, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau 1 phút hoặc liên hệ ban quản lý."],
    [undefined, "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút hoặc liên hệ ban quản lý."],
  ])("retryAfter %s", (retryAfter, text) => {
    expect(thongBaoKhoaTam(retryAfter)).toBe(text);
    expect(
      messageFor(new ApiError({ status: 429, errorCode: "AUTH_TOO_MANY_ATTEMPTS", message: "x", retryAfter })),
    ).toBe(text);
  });
});

describe("mã lỗi căn hộ và hồ sơ cư trú (spec 0014, 0015)", () => {
  // covers: AC-6, AC-8, AC-10, AC-17 (spec 0014); AC-3, AC-4, AC-5, AC-6, AC-7, AC-17 (spec 0015)
  it.each([
    [422, "CANHO_TRANG_THAI_MAU_THUAN_CU_TRU", "Trạng thái sử dụng không khớp với hồ sơ cư trú hiện tại."],
    [
      422,
      "CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG",
      "Tài khoản cư dân đã ngừng hoạt động, không thể ghi nhận hoặc duyệt hồ sơ cư trú.",
    ],
    [409, "CUTRU_TRUNG_KHOANG_NGAY_CU_DAN", "Cư dân đã có hồ sơ cư trú hợp lệ giao khoảng ngày tại căn hộ này."],
    [409, "CUTRU_DA_CO_CHU_CAN_HO", "Căn hộ đã có chủ căn hộ hợp lệ trong khoảng ngày này."],
    [409, "CUTRU_HO_SO_DA_HET_HIEU_LUC", "Hồ sơ cư trú đã hết hiệu lực, không thể chỉnh sửa."],
    [409, "CUTRU_HO_SO_DANG_CHO_XAC_MINH", "Hồ sơ đang chờ xác minh, chưa thể chỉnh sửa tại đây."],
    [409, "CUTRU_DE_NGHI_DA_KET_THUC", "Đề nghị cư trú đã kết thúc xử lý, không thể chỉnh sửa."],
    [
      422,
      "CUTRU_KHONG_KHOP_CU_DAN",
      "Không tìm thấy tài khoản cư dân khớp đồng thời số CCCD và số điện thoại đã nhập.",
    ],
    [409, "CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH", "Cư dân này đã có đề nghị cư trú đang chờ xác minh tại căn hộ."],
    [
      422,
      "CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH",
      "Căn hộ đang có tối đa 10 đề nghị chờ xác minh, vui lòng chờ Ban quản lý xử lý hoặc hủy đề nghị cũ.",
    ],
    [409, "CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH", "Đề nghị cư trú không còn ở trạng thái chờ xác minh."],
    [
      422,
      "CUTRU_DE_NGHI_DA_QUA_NGAY_KET_THUC",
      "Đề nghị cư trú đã qua ngày kết thúc, vui lòng từ chối để cư dân gửi lại.",
    ],
  ])("ánh xạ %s %s sang đúng thông báo tiếng Việt thay vì message thô", (status, errorCode, expected) => {
    expect(messageFor(error(status, errorCode))).toBe(expected);
  });
});

// spec 0016, AC-16
describe("mã lỗi phương tiện", () => {
  it.each([
    [422, "PHUONGTIEN_CHU_XE_KHONG_CU_TRU"],
    [422, "PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG"],
    [422, "PHUONGTIEN_VUOT_SO_DON_CHO_DUYET"],
    [409, "PHUONGTIEN_TRUNG_BIEN_SO"],
    [409, "PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE"],
    [409, "PHUONGTIEN_DON_KHONG_CON_CHO_DUYET"],
  ])("%s %s có câu riêng, không rơi về câu chung", (status, ma) => {
    expect(messageFor(error(status, ma))).not.toBe(messageFor(error(status, "XYZ")));
  });
});
