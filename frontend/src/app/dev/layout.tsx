import { notFoundInProduction } from "./dev-only";

/** Mọi trang dưới /dev/ chỉ có khi chạy `pnpm dev`; bản production trả 404 (AC-14). */
export default function DevLayout({ children }: LayoutProps<"/dev">) {
  notFoundInProduction();
  return children;
}
