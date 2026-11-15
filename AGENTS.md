# Hệ thống Quản lý Chung cư

## Stack

- **Language / Runtime**: Java 25 (backend), TypeScript `strict` trên Node.js 24 (frontend)
- **Framework**: Spring Boot 4.1 (modular monolith, package gốc `vn.edu.hust.chungcu`), Next.js 16 App Router + React 19
- **Key dependencies**: Spring Data JPA, Flyway, Spring Security (tự ký JWT), springdoc 3.x; TanStack Query, openapi-fetch, Tailwind CSS v4 + shadcn/ui; PostgreSQL 18 trong Docker
- **Package manager**: Maven Wrapper (`mvnw.cmd`) cho backend, pnpm 12.17.0 cho frontend

Chi tiết (hợp đồng API, mã lỗi, phân trang, ngày giờ, quy ước DB) ở [spec 0001](docs/specs/0001-stack-kien-truc/index.md); spec thắng khi mâu thuẫn.

## Build approach

**Tracer Bullet**: mỗi lát cắt chạy thật xuyên suốt DB, API và giao diện; lát sau làm dày thêm một đoạn của đường đã chạy.

## Commands

```bash
docker compose up -d                          # PostgreSQL dev, cổng 5433
cd backend && mvnw.cmd spring-boot:run        # backend :8080, Swagger ở /swagger-ui.html (dev)
cd backend && mvnw.cmd verify                 # build + test (cần Docker), ghi docs/api/openapi.json
cd frontend && pnpm install && pnpm dev       # frontend :3000, gọi /api qua rewrites
cd frontend && pnpm gen:api && pnpm typecheck && pnpm build   # sinh kiểu API rồi kiểm tra
cd frontend && pnpm lint && pnpm format:check   # sửa nhanh: pnpm lint:fix, pnpm format
cd backend && mvnw.cmd spring-javaformat:apply  # tự format Java khi validate báo lỗi
docker compose -f docker-compose.prod.yml up --build   # bản demo cả ba phần
```

## Specs

`docs/specs/NNNN-title.md` hoặc `docs/specs/NNNN-title/index.md`; scope ở `docs/scope/scope.md`.

## Rules

- SOLID OOP: mỗi lớp một trách nhiệm (dưới khoảng 200 dòng), inject qua constructor, không service locator, ưu tiên composition hơn kế thừa quá một tầng, interface nhỏ và chỉ tạo khi có hơn một cách cài hoặc cần thay trong test.
- Mỗi phân hệ là một package ngang hàng (`auth`, `canho`, ...), bên trong chia controller, service, repository, dto, entity. Phân hệ khác chỉ gọi qua service công khai, không đọc repository của nhau. Controller trả DTO hoặc record, không trả entity; service giữ transaction.
- Lỗi: service ném `ApiException` (hoặc lớp con) với `errorCode` cố định viết hoa, mã riêng có tiền tố phân hệ (`AUTH_...`). Mọi lỗi đi qua `GlobalExceptionHandler`; không bắt lỗi rồi tự dựng response. Frontend gọi API chỉ qua hàm `api` và xử lý `ApiError` theo `errorCode`, không theo `message`. Thành công bọc bằng `ApiResponse.ok/created`. Lấy giờ qua bean `Clock`, không gọi `Instant.now()` trong nghiệp vụ.
- Đặt tên: khái niệm nghiệp vụ dùng tiếng Việt không dấu (`CanHoService`, `maCanHo`, bảng `can_ho`), khái niệm kỹ thuật dùng tiếng Anh (`Controller`, `Repository`, `Dto`). Component React PascalCase, hook `useXxx`. Thông báo cho người dùng viết tiếng Việt có dấu, lưu UTF-8 NFC.
- Giao diện đạt WCAG AA: dùng được hoàn toàn bằng bàn phím, focus nhìn thấy được, mọi ô nhập có nhãn, lỗi gắn với ô nhập, đủ tương phản.
- Test: unit test cho service (inject fake qua constructor), integration test với PostgreSQL thật qua Testcontainers cho API và repository (không dùng H2). Frontend: Vitest + Testing Library.
- Commit theo Conventional Commits, phạm vi là phân hệ: `feat(auth): ...`, `fix(common): ...`, `chore(tooling): ...`.

## Tooling

Đã cài. Cấu hình ở `frontend/eslint.config.mjs`, `frontend/.prettierrc.json`, `lefthook.yml`, `.github/workflows/ci.yml` và plugin trong `backend/pom.xml`.
- Backend: Spring Java Format (`spring-javaformat-maven-plugin`, thụt lề tab), `validate` chặn build, `apply` để tự format.
- Frontend: ESLint flat config (`eslint-config-next`, `jsx-a11y`) + Prettier (`eslint-config-prettier`); script `lint`, `format`, `format:check`.
- Pre-commit: Lefthook ở gốc repo, chạy ESLint, Prettier, `tsc` cho frontend và Spring Java Format validate cho backend. Không chạy test.
- Lefthook là devDependency của `frontend`; hook được cài khi chạy `pnpm install` trong `frontend/` (script `prepare`). CI tắt hook bằng `LEFTHOOK=0`.
- `pnpm typecheck` chạy `next typegen` trước `tsc`, vì kiểu route như `LayoutProps` do Next.js sinh ra và không có trên checkout mới.
- Giữ ESLint 9 cho tới khi `eslint-plugin-react`, `eslint-plugin-import` và `eslint-plugin-jsx-a11y` hỗ trợ ESLint 10.
- CI: GitHub Actions khi push và PR: `mvnw verify`, lint, typecheck, build frontend, sinh lại `openapi.json` rồi `git diff --exit-code`.

## Git

- integration: on
- branch prefix: feat/
- commit: per-milestone

## Agent skills

- [java-springboot](.agents/skills/java-springboot/): `github/awesome-copilot`, quy ước Spring Boot 4 (controller, JPA, cấu hình). Riêng lỗi thì theo vỏ bọc của spec 0001, không theo `ProblemDetail`.
- [next-dev-loop](.agents/skills/next-dev-loop/): `vercel/next.js`, kiểm tra hành vi Next.js khi `pnpm dev` đang chạy.
- [ui-styling](.claude/skills/ui-styling/): Tailwind CSS + shadcn/ui.
- [design-system](.claude/skills/design-system/): token và component spec, dùng ở tính năng 4.

Declined: thêm skill cho ESLint, Prettier, Lefthook, Spring Java Format, GitHub Actions · MCP servers: next-devtools (connected, cần `pnpm dev` đang chạy)

## Context files

- [frontend/AGENTS.md](frontend/AGENTS.md) (do `next dev` tự ghi: đọc tài liệu Next.js 16 trong `node_modules/next/dist/docs/` trước khi viết code)

_Drafted by /audit from the repo, worth a quick human pass. Edit freely: once a line stops matching this draft, later runs treat it as curated and will flag rather than overwrite it._
