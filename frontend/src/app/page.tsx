import { redirect } from "next/navigation";

// Tính năng 7 thay bằng trang chủ theo vai trò
export default function Home() {
  redirect("/ho-so");
}
