import { QueryClientProvider, useQuery } from "@tanstack/react-query";
import { render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ApiError } from "./api";
import { createQueryClient } from "./query-client";

function Probe({ queryFn }: { queryFn: () => Promise<string> }) {
  const query = useQuery({ queryKey: ["probe"], queryFn, retryDelay: 0 });
  return <p>{query.status}</p>;
}

function run(status: number) {
  const queryFn = vi.fn(() => Promise.reject(new ApiError({ status, errorCode: "X", message: "x" })));
  render(
    <QueryClientProvider client={createQueryClient()}>
      <Probe queryFn={queryFn} />
    </QueryClientProvider>,
  );
  return queryFn;
}

describe("QueryClient thử lại (AC-19)", () => {
  it("lỗi 404 không thử lại", async () => {
    const queryFn = run(404);
    await waitFor(() => expect(screen.getByText("error")).toBeInTheDocument());
    expect(queryFn).toHaveBeenCalledTimes(1);
  });

  it("lỗi 500 thử lại đúng 1 lần", async () => {
    const queryFn = run(500);
    await waitFor(() => expect(screen.getByText("error")).toBeInTheDocument());
    expect(queryFn).toHaveBeenCalledTimes(2);
  });
});
