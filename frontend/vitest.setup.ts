import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";

afterEach(() => {
  cleanup();
});

// Polyfill cho Radix trên jsdom
class ResizeObserverStub {
  observe() {}
  unobserve() {}
  disconnect() {}
}
globalThis.ResizeObserver ??= ResizeObserverStub as unknown as typeof ResizeObserver;
Element.prototype.hasPointerCapture ??= () => false;
Element.prototype.releasePointerCapture ??= () => {};
Element.prototype.scrollIntoView ??= () => {};
window.matchMedia ??= (query: string) =>
  ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false,
  }) as MediaQueryList;

// next/navigation: test điều khiển qua src/test/next-navigation.ts
vi.mock("next/navigation", async () => {
  const { mockPathname, mockNotFound } = await import("./src/test/next-navigation");
  return {
    usePathname: () => mockPathname(),
    useRouter: () => ({ push: vi.fn(), replace: vi.fn(), back: vi.fn(), refresh: vi.fn(), prefetch: vi.fn() }),
    useSearchParams: () => new URLSearchParams(),
    notFound: mockNotFound,
  };
});

vi.mock("next/font/google", () => {
  const font = () => ({ className: "font-mock", variable: "--font-mock", style: { fontFamily: "mock" } });
  return { Be_Vietnam_Pro: font };
});
