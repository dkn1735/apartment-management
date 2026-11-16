import { APP_NAME } from "@/lib/brand";

// Trang tạm của bộ khung. Giao diện thật của trang chủ đến ở tính năng 5 (đăng nhập) và 7 (bố cục app).
export default function Home() {
  return (
    <main className="mx-auto max-w-xl px-4 py-10 sm:px-6">
      <h1 className="type-page-title">{APP_NAME}</h1>
      <p className="mt-2 text-muted-foreground">Bộ khung đã chạy. Các màn hình sẽ được thêm theo từng tính năng.</p>
    </main>
  );
}
