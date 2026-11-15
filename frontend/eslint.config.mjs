import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";
import jsxA11y from "eslint-plugin-jsx-a11y";
import prettier from "eslint-config-prettier/flat";

export default defineConfig([
  ...nextVitals,
  ...nextTs,
  // WCAG AA (AGENTS.md): bật đủ bộ luật recommended của jsx-a11y.
  // Plugin đã được eslint-config-next khai báo, nên ở đây chỉ thêm luật.
  {
    files: ["**/*.{jsx,tsx}"],
    rules: jsxA11y.flatConfigs.recommended.rules,
  },
  {
    rules: {
      // Biến hoặc tham số cố ý bỏ qua thì đặt tên bắt đầu bằng _
      "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_", varsIgnorePattern: "^_" }],
    },
  },
  // Prettier lo định dạng: tắt mọi luật ESLint đụng tới định dạng. Luôn để cuối.
  prettier,
  globalIgnores([".next/**", "out/**", "build/**", "next-env.d.ts", "src/lib/api/schema.d.ts"]),
]);
