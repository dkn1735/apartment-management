import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { mockNotFound } from "@/test/next-navigation";
import DevLayout from "./layout";

type Props = Parameters<typeof DevLayout>[0];
const props = (children: string) => ({ children, params: Promise.resolve({}) }) as unknown as Props;

describe("dev/layout (AC-14)", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    mockNotFound.mockClear();
  });

  it("gọi notFound khi chạy production", () => {
    vi.stubEnv("NODE_ENV", "production");
    expect(() => render(<DevLayout {...props("bí mật")} />)).toThrow("NEXT_NOT_FOUND");
    expect(mockNotFound).toHaveBeenCalled();
  });

  it("hiện trang khi chạy dev", () => {
    vi.stubEnv("NODE_ENV", "development");
    render(<DevLayout {...props("trang dev")} />);
    expect(screen.getByText("trang dev")).toBeInTheDocument();
    expect(mockNotFound).not.toHaveBeenCalled();
  });
});
