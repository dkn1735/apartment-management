import { ApiError } from "@/lib/api/api";

/** Các tình huống hàm gửi giả trả về, để thử cách form xử lý lỗi API. */
export const SCENARIOS = [
  { value: "ok", label: "Thành công" },
  { value: "validation", label: "VALIDATION_FAILED theo trường" },
  { value: "conflict", label: "CONFLICT" },
  { value: "internal", label: "INTERNAL_ERROR có mã tra cứu" },
  { value: "unavailable", label: "BACKEND_UNAVAILABLE" },
] as const;

export type Scenario = (typeof SCENARIOS)[number]["value"];

/** Câu `message` chung của response giả; giao diện không bao giờ được hiện câu này. */
export const RAW_MESSAGE = "Raw backend message, must never be shown";

const DELAY_MS = 800;

/** Hàm gửi giả: chờ một lúc rồi thành công hoặc ném `ApiError` theo tình huống. */
export async function fakeSubmit(scenario: Scenario, delayMs = DELAY_MS): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, delayMs));
  switch (scenario) {
    case "ok":
      return;
    case "validation":
      throw new ApiError({
        status: 400,
        errorCode: "VALIDATION_FAILED",
        message: RAW_MESSAGE,
        errors: [
          { field: "soDienThoai", message: "Số điện thoại đã được dùng cho tài khoản khác." },
          { field: "maLa", message: "Mã lạ không khớp ô nào nên hiện ở khung lỗi đầu form." },
        ],
      });
    case "conflict":
      throw new ApiError({ status: 409, errorCode: "CONFLICT", message: RAW_MESSAGE });
    case "internal":
      throw new ApiError({
        status: 500,
        errorCode: "INTERNAL_ERROR",
        message: RAW_MESSAGE,
        requestId: "a1b2c3d4-5e6f-4a7b-8c9d-0e1f2a3b4c5d",
      });
    case "unavailable":
      throw new ApiError({
        status: 502,
        errorCode: "BACKEND_UNAVAILABLE",
        message: RAW_MESSAGE,
        requestId: "f0e1d2c3-b4a5-4968-8776-655443322110",
      });
  }
}
