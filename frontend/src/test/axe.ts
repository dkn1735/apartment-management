import axe from "axe-core";

/**
 * Chạy axe-core trên một vùng DOM và báo lỗi kèm danh sách vi phạm.
 * Tắt `color-contrast` vì jsdom không tính được màu; tương phản do test bảng tương phản lo.
 */
export async function expectNoA11yViolations(container: Element): Promise<void> {
  const result = await axe.run(container, { rules: { "color-contrast": { enabled: false } } });
  if (result.violations.length > 0) {
    const report = result.violations
      .map((v) => `${v.id} (${v.impact}): ${v.help}\n  ${v.nodes.map((n) => n.html).join("\n  ")}`)
      .join("\n");
    throw new Error(`Có ${result.violations.length} vi phạm trợ năng:\n${report}`);
  }
}
