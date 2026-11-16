import type { Metadata } from "next";
import { HoSoView } from "./ho-so-view";

export const metadata: Metadata = { title: "Hồ sơ của tôi" };

export default function HoSoPage() {
  return <HoSoView />;
}
