import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// Mọi test chạy ở múi giờ khác Việt Nam, để bắt lỗi định dạng ngày phụ thuộc máy (spec 0003, Hạ tầng test).
// Đặt ở đây vì đặt bằng biến môi trường trong lệnh không ổn định trên Windows.
process.env.TZ = "America/New_York";

export default defineConfig({
  plugins: [react()],
  // Vite tự đọc alias @/* trong tsconfig.json
  resolve: { tsconfigPaths: true },
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
    css: false,
  },
});
