import { notFound } from "next/navigation";

/**
 * Chặn trang dev ở bản production (AC-14). Next.js render layout, page và metadata của một route song song,
 * nên gọi ở layout thôi thì nội dung page vẫn lọt vào payload RSC của trang 404. Mọi layout, page và
 * generateMetadata dưới /dev/ đều phải gọi hàm này đầu tiên.
 */
export function notFoundInProduction(): void {
  if (process.env.NODE_ENV === "production") {
    notFound();
  }
}
