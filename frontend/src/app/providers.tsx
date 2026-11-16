"use client";

import { QueryClientProvider } from "@tanstack/react-query";
import { useState, type ReactNode } from "react";
import { Toaster } from "@/components/ui/sonner";
import { createQueryClient } from "@/lib/api/query-client";

export function Providers({ children }: { children: ReactNode }) {
  // Mỗi phiên trình duyệt một QueryClient, tạo một lần
  const [queryClient] = useState(createQueryClient);

  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <Toaster />
    </QueryClientProvider>
  );
}
