import type { ApiError } from "./api";

/**
 * Từ điển errorCode sang câu tiếng Việt (spec 0003, mục Xử lý lỗi API).
 * Mã riêng của phân hệ (AUTH_...) được tính năng sở hữu nó thêm vào đây.
 */
export const ERROR_MESSAGES: Readonly<Record<string, string>> = {
  VALIDATION_FAILED: "Dữ liệu chưa hợp lệ, vui lòng kiểm tra các ô được đánh dấu.",
  MALFORMED_REQUEST: "Yêu cầu không hợp lệ, vui lòng tải lại trang và thử lại.",
  METHOD_NOT_ALLOWED: "Thao tác này không được hỗ trợ.",
  UNSUPPORTED_MEDIA_TYPE: "Định dạng dữ liệu gửi lên không được hỗ trợ.",
  UNAUTHENTICATED: "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.",
  FORBIDDEN: "Bạn không có quyền thực hiện thao tác này.",
  NOT_FOUND: "Không tìm thấy dữ liệu yêu cầu, có thể nó đã bị xóa.",
  CONFLICT: "Dữ liệu bị trùng hoặc đang ở trạng thái không cho phép thao tác này.",
  CONCURRENT_MODIFICATION: "Dữ liệu vừa được người khác thay đổi, vui lòng tải lại và thử lại.",
  PAYLOAD_TOO_LARGE: "Tệp quá lớn, giới hạn là 20MB.",
  BUSINESS_RULE_VIOLATION: "Thao tác vi phạm quy định của hệ thống.",
  INTERNAL_ERROR: "Hệ thống đang gặp sự cố, vui lòng thử lại sau.",
  BACKEND_UNAVAILABLE: "Không kết nối được tới máy chủ, vui lòng thử lại sau ít phút.",
  CSRF_REJECTED: "Yêu cầu bị từ chối vì lý do bảo mật. Vui lòng tải lại trang và thử lại.",
  // Phân hệ xác thực (spec 0004); backend dùng cùng các câu này làm message
  AUTH_INVALID_CREDENTIALS: "Số điện thoại, email hoặc mật khẩu không đúng.",
  AUTH_ACCOUNT_LOCKED: "Tài khoản đang bị khóa hoặc đã ngừng hoạt động. Vui lòng liên hệ ban quản lý.",
  // Spec 0005: mọi lý do làm mới thất bại dùng chung câu này
  AUTH_REFRESH_INVALID: "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.",
  // API-AUTH-04: đổi mật khẩu
  AUTH_OLD_PASSWORD_INCORRECT: "Mật khẩu hiện tại không đúng.",
  // Spec 0008: quản lý tài khoản
  AUTH_IDENTITY_TAKEN: "Thông tin đã được dùng cho tài khoản khác.",
  AUTH_ROLE_IN_USE:
    "Vai trò này đang gắn với dữ liệu khác nên không gỡ được. Nếu người này không còn làm việc, hãy chuyển tài khoản sang Ngừng hoạt động.",
  AUTH_LAST_ROLE:
    "Tài khoản phải có ít nhất một vai trò. Nếu người này không còn dùng hệ thống, hãy chuyển sang Ngừng hoạt động.",
  AUTH_SELF_PROTECTED: "Bạn không thể tự gỡ vai trò quản trị, tự khóa hay tự đặt lại mật khẩu của mình.",
  AUTH_LAST_ADMIN: "Hệ thống phải luôn còn ít nhất một quản trị viên đang hoạt động.",
  AUTH_PASSWORD_CHANGE_REQUIRED: "Bạn cần đổi mật khẩu tạm trước khi tiếp tục.",
  // Spec 0009: khóa tạm đăng nhập; có Retry-After thì dùng thongBaoKhoaTam để nêu số phút
  AUTH_TOO_MANY_ATTEMPTS: "Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ít phút hoặc liên hệ ban quản lý.",
  // Spec 0010: mọi lý do link đặt lại mật khẩu không dùng được dùng chung câu này
  AUTH_RESET_TOKEN_INVALID: "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn. Vui lòng yêu cầu link mới.",
  // Spec 0011, 0012, 0013, 0014: tòa nhà, tầng và căn hộ
  CANHO_TOA_NHA_DA_TON_TAI: "Tòa nhà đã tồn tại.",
  CANHO_TANG_SAI_THU_TU: "Tầng mới phải nối tiếp tầng cao nhất của tòa.",
  CANHO_CAN_HO_DA_TON_TAI: "Căn hộ đã tồn tại.",
  CANHO_TIEN_ICH_KHONG_DOI: "Trạng thái tiện ích của căn hộ đã thay đổi, vui lòng tải lại.",
  CANHO_TRANG_THAI_MAU_THUAN_CU_TRU: "Trạng thái sử dụng không khớp với hồ sơ cư trú hiện tại.",
  // Spec 0014, 0015: hồ sơ cư trú căn hộ và đề nghị cư trú
  CUTRU_TAI_KHOAN_NGUNG_HOAT_DONG: "Tài khoản cư dân đã ngừng hoạt động, không thể ghi nhận hoặc duyệt hồ sơ cư trú.",
  CUTRU_TRUNG_KHOANG_NGAY_CU_DAN: "Cư dân đã có hồ sơ cư trú hợp lệ giao khoảng ngày tại căn hộ này.",
  CUTRU_DA_CO_CHU_CAN_HO: "Căn hộ đã có chủ căn hộ hợp lệ trong khoảng ngày này.",
  CUTRU_HO_SO_DA_HET_HIEU_LUC: "Hồ sơ cư trú đã hết hiệu lực, không thể chỉnh sửa.",
  CUTRU_HO_SO_DANG_CHO_XAC_MINH: "Hồ sơ đang chờ xác minh, chưa thể chỉnh sửa tại đây.",
  CUTRU_DE_NGHI_DA_KET_THUC: "Đề nghị cư trú đã kết thúc xử lý, không thể chỉnh sửa.",
  CUTRU_KHONG_KHOP_CU_DAN: "Không tìm thấy tài khoản cư dân khớp đồng thời số CCCD và số điện thoại đã nhập.",
  CUTRU_DA_CO_DE_NGHI_CHO_XAC_MINH: "Cư dân này đã có đề nghị cư trú đang chờ xác minh tại căn hộ.",
  CUTRU_VUOT_SO_DE_NGHI_CHO_XAC_MINH:
    "Căn hộ đang có tối đa 10 đề nghị chờ xác minh, vui lòng chờ Ban quản lý xử lý hoặc hủy đề nghị cũ.",
  CUTRU_DE_NGHI_KHONG_CON_CHO_XAC_MINH: "Đề nghị cư trú không còn ở trạng thái chờ xác minh.",
  CUTRU_DE_NGHI_DA_QUA_NGAY_KET_THUC: "Đề nghị cư trú đã qua ngày kết thúc, vui lòng từ chối để cư dân gửi lại.",
  // Spec 0016: phương tiện gửi xe căn hộ
  PHUONGTIEN_CHU_XE_KHONG_CU_TRU: "Chủ xe không đang cư trú hợp lệ tại căn hộ này.",
  PHUONGTIEN_TAI_KHOAN_NGUNG_HOAT_DONG:
    "Tài khoản của chủ xe đã ngừng hoạt động, không thể đăng ký hoặc kích hoạt phương tiện.",
  PHUONGTIEN_VUOT_SO_DON_CHO_DUYET:
    "Căn hộ đang có tối đa 10 đơn đăng ký xe chờ duyệt, vui lòng chờ Ban quản lý xử lý hoặc rút đơn cũ.",
  PHUONGTIEN_TRUNG_BIEN_SO: "Biển số này đang được đăng ký cho một phương tiện khác.",
  PHUONGTIEN_CHUYEN_TRANG_THAI_KHONG_HOP_LE:
    "Không thể chuyển phương tiện từ trạng thái hiện tại sang trạng thái đã chọn.",
  PHUONGTIEN_DON_KHONG_CON_CHO_DUYET: "Đơn đăng ký phương tiện không còn ở trạng thái chờ duyệt.",
};

/** Câu khi bị khóa tạm, có số phút làm tròn lên từ `Retry-After` (spec 0009, AC-13); thiếu thì câu mặc định. */
export function thongBaoKhoaTam(retryAfter: number | undefined): string {
  if (retryAfter === undefined || !Number.isFinite(retryAfter) || retryAfter <= 0) {
    return ERROR_MESSAGES.AUTH_TOO_MANY_ATTEMPTS;
  }
  const phut = Math.max(1, Math.ceil(retryAfter / 60));
  return `Bạn đã nhập sai quá nhiều lần. Vui lòng thử lại sau ${phut} phút hoặc liên hệ ban quản lý.`;
}

/** Câu chung theo mã HTTP khi errorCode chưa có trong từ điển. */
function messageForStatus(status: number): string {
  switch (status) {
    case 400:
    case 422:
      return "Dữ liệu chưa hợp lệ, vui lòng kiểm tra lại.";
    case 401:
      return "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại.";
    case 403:
      return "Bạn không có quyền thực hiện thao tác này.";
    case 404:
      return "Không tìm thấy dữ liệu yêu cầu.";
    case 409:
      return "Dữ liệu đã thay đổi hoặc bị trùng, vui lòng tải lại và thử lại.";
    case 413:
      return "Tệp quá lớn.";
    default:
      return "Hệ thống đang gặp sự cố, vui lòng thử lại sau.";
  }
}

/** Câu thông báo chung cho một lỗi API. Không bao giờ dùng `error.message` của response. */
export function messageFor(error: ApiError): string {
  if (error.errorCode === "AUTH_TOO_MANY_ATTEMPTS") return thongBaoKhoaTam(error.retryAfter);
  return Object.hasOwn(ERROR_MESSAGES, error.errorCode)
    ? ERROR_MESSAGES[error.errorCode]
    : messageForStatus(error.status);
}

/** Lỗi hệ thống (5xx, mất kết nối) có requestId thì hiện mã tra cứu; lỗi 4xx thì không (AC-7). */
export function requestIdToShow(error: ApiError): string | undefined {
  const isSystemError = error.status >= 500 || error.errorCode === "BACKEND_UNAVAILABLE";
  return isSystemError ? error.requestId || undefined : undefined;
}
