"use client";

import { Be_Vietnam_Pro } from "next/font/google";
import { useEffect } from "react";
import { UnexpectedError } from "@/components/feedback/unexpected-error";
import { APP_NAME } from "@/lib/brand";
import "./globals.css";

const beVietnamPro = Be_Vietnam_Pro({
  subsets: ["vietnamese", "latin"],
  weight: ["200", "400", "500", "600"],
  display: "swap",
  variable: "--font-sans",
});

/** Bắt lỗi ở chính root layout; thay cả root layout nên tự có html, body, font và style. */
export default function GlobalError({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <html lang="vi" className={beVietnamPro.variable}>
      <body>
        <title>{`Đã có lỗi xảy ra · ${APP_NAME}`}</title>
        <UnexpectedError error={error} retry={retry} />
      </body>
    </html>
  );
}
