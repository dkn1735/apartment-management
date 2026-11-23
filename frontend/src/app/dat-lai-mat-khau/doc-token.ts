/**
 * Đọc token từ fragment `#token=...` rồi xóa fragment khỏi thanh địa chỉ (spec 0010, AC-13), để token không nằm lại
 * trong lịch sử hay bị chép khi người dùng chia sẻ URL. Không có token thì trả `null`.
 */
export function docVaXoaToken(): string | null {
  const hash = window.location.hash;
  if (!hash) return null;
  const token = new URLSearchParams(hash.slice(1)).get("token");
  window.history.replaceState(window.history.state, "", window.location.pathname + window.location.search);
  return token && token.length > 0 ? token : null;
}
