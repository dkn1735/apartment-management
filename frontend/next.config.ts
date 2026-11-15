import type { NextConfig } from "next";

// rewrites được đóng cứng vào bản build lúc `next build`; đổi BACKEND_URL lúc chạy không có tác dụng.
const backendUrl = process.env.BACKEND_URL ?? "http://127.0.0.1:8080";

const nextConfig: NextConfig = {
  // Image demo chạy bằng `node server.js` từ bản standalone
  output: "standalone",
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  // Trình duyệt chỉ gọi /api/... trên tên miền của Next.js; Next.js chuyển tiếp sang Spring Boot.
  // Vì vậy không đặt Route Handler nào dưới src/app/api (rewrite sẽ nuốt mất).
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
