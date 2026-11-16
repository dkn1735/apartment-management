"use client";

import { useQuery } from "@tanstack/react-query";
import { api, client } from "@/lib/api/api";
import type { components } from "@/lib/api/schema";

export type HoSo = components["schemas"]["HoSoResponse"];

/** Khóa query hồ sơ; layout `(app)` và trang `/ho-so` dùng chung một lần gọi. */
export const HO_SO_QUERY_KEY = ["ho-so"] as const;

/** Hồ sơ của người đang đăng nhập (`GET /api/v1/auth/profile`). */
export function useHoSo() {
  return useQuery({
    queryKey: HO_SO_QUERY_KEY,
    queryFn: () => api<HoSo>(client.GET("/api/v1/auth/profile")),
  });
}
