import * as z from "zod";

/** Quy tắc độ mạnh của mật khẩu mới (API-AUTH-04, spec 0010 AC-9); backend kiểm lại, đây chỉ để báo sớm. */
export const QUY_TAC_MAT_KHAU_MOI = "Ít nhất 8 ký tự, có cả chữ và số.";

/** Ô mật khẩu mới, dùng chung cho đổi mật khẩu và đặt lại mật khẩu qua email. */
export const matKhauMoiSchema = z
  .string()
  .min(1, "Vui lòng nhập mật khẩu mới")
  .min(8, "Mật khẩu mới phải có từ 8 đến 128 ký tự")
  .max(128, "Mật khẩu mới phải có từ 8 đến 128 ký tự")
  .regex(/\p{L}/u, "Mật khẩu mới phải có cả chữ và số")
  .regex(/\p{Nd}/u, "Mật khẩu mới phải có cả chữ và số");

/** Link đặt lại mật khẩu sống bao lâu; cùng giá trị với `app.security.password-reset-ttl` của backend. */
export const THOI_HAN_LINK_PHUT = 30;
