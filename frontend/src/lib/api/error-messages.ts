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
};

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
  return Object.hasOwn(ERROR_MESSAGES, error.errorCode)
    ? ERROR_MESSAGES[error.errorCode]
    : messageForStatus(error.status);
}

/** Lỗi hệ thống (5xx, mất kết nối) có requestId thì hiện mã tra cứu; lỗi 4xx thì không (AC-7). */
export function requestIdToShow(error: ApiError): string | undefined {
  const isSystemError = error.status >= 500 || error.errorCode === "BACKEND_UNAVAILABLE";
  return isSystemError ? error.requestId || undefined : undefined;
}
