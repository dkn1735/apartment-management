# Stack discovery (2026-11-16)

Source: `npx skills find` (skills.sh install counts) + web search for MCP servers.
Excluded per brief: ui-styling, design-system, ui-ux-pro-max, frontend-design, jsmastery workflow skills, supabase skills, Context7, Playwright, Supabase, Stripe MCP.

## Spring Boot / Spring Framework
- affaan-m/ecc@springboot-patterns (14.9K) — Spring Boot patterns. Popular, unknown owner.
- affaan-m/ecc@springboot-security (13.6K) — security patterns. Popular, unknown owner.
- github/awesome-copilot@java-springboot (22K) — Spring Boot Java guidance. Popular (GitHub-owned repo).
- jabrena/plinth@304-frameworks-spring-boot-security (322) — security. Popular-ish.
- jeffallan/claude-skills@spring-boot-engineer (8.8K) — broad Spring Boot. Popular, unknown.
- mindrally/skills@spring-framework (767) — Spring Framework. Low-medium.

## Spring Security (JWT)
- giuseppe-trisciuoglio/developer-kit@spring-boot-security-jwt (4.3K) — JWT specifically. Popular, unknown.
- github/awesome-copilot@spring-security (22) — low installs, GitHub-owned repo.
- jetbrains/skills@spring-security-configurator-auditor (49) — JetBrains-owned; audit focus.

## Java 25 / Maven
- ricardolv/java-25-skills@java-25 (9) — low installs, unknown.
- decebals/claude-code-java@java-migration (370) — Java migration. Low-medium.
- pluginagentmarketplace/custom-plugin-java@java-maven (915) — Maven. Popular, unknown.
- jabrena/plinth@110-java-maven-best-practices (506) — Maven best practices. Popular-ish.
- No official Java 25 skill found.

## Spring Data JPA / Hibernate
- prabhatkrmishra/spring-boot-production-skills@spring-boot-jpa (1) — low.
- edimilsonldutra/spring-boot-core@spring-data-jpa (1) — low.
- adityamparikh/hibernate-jpa-validator-skill@hibernate-jpa-validator (6) — low.
- Weak coverage; no official Hibernate or Spring Data skill found.

## Flyway
- jabrena/plinth@313-frameworks-spring-db-migrations-flyway (381) — Spring + Flyway. Popular-ish.
- rrezartprebreza/spring-boot-skills@flyway-migrations (155) — low-medium, unknown.

## PostgreSQL 18
- github/awesome-copilot@postgresql-optimization (17.1K) — popular, GitHub-owned repo.
- wshobson/agents@postgresql-table-design (29K) — popular, unknown.
- microsoft/postgres-skills@postgresql-best-practices (356) — Microsoft-owned repo, low installs.
- Note: supabase/agent-skills@supabase-postgres-best-practices (486.7K) is excluded per brief.

## springdoc-openapi
- giuseppe-trisciuoglio/developer-kit@spring-boot-openapi-documentation (4.2K) — popular, unknown.
- claude-dev-suite/claude-dev-suite@springdoc-openapi (56) — low.

## Next.js 16 (App Router)
- vercel/next.js (official): vercel/next.js@next-dev-loop (25.8K), next-cache-components-adoption (27.7K), next-cache-components-optimizer (21.3K). Official (Vercel repo).
- vercel-labs/agent-skills@next-best-practices (74) — official Vercel Labs, low installs.
- cloudflare/skills@nextjs-on-cloudflare (59.4K) — Cloudflare-owned; only relevant if deploying on Cloudflare.
- giuseppe-trisciuoglio/developer-kit@nextjs-app-router (4K) — popular, unknown.

## React 19
- github/awesome-copilot@react19-concurrent-patterns (1.1K) — GitHub-owned.
- claude-dev-suite/claude-dev-suite@react-19 (58) — low.
- Note: "react-19" hits are mostly low-install community skills.

## TypeScript
- wshobson/agents@typescript-advanced-types (100.1K) — popular, unknown owner.
- jeffallan/claude-skills@typescript-pro (14.8K) — popular, unknown.
- cursor/plugins@typescript-best-practices (10.8K) — Cursor-owned.

## TanStack Query
- tanstack-skills/tanstack-skills@tanstack-query (10.5K) — TanStack-named org; closest to official. Verify org ownership.
- deckardger/tanstack-agent-skills@tanstack-query-best-practices (13.4K) — popular, unknown.

## React Hook Form
- pproenca/dot-skills@react-hook-form (3.1K) — popular, unknown.
- jezweb/claude-skills@react-hook-form-zod (1.2K) — combined RHF + Zod. Popular-ish.

## Zod
- pproenca/dot-skills@zod (7.3K) — popular, unknown.
- giuseppe-trisciuoglio/developer-kit@zod-validation-utilities (4K) — popular, unknown.

## openapi-typescript / openapi-fetch
- prescottprue/agent-skills@openapi-fetch (3) — low.
- Weak coverage. Ignore lark-openapi-explorer (unrelated Feishu/Lark APIs).

## pnpm
- antfu/skills@pnpm (23.1K) — popular, Anthony Fu (well known maintainer), unknown-official.
- hairyf/skills@pnpm (236) — low-medium.

## Docker Compose
- docker/skills@docker-compose-patterns (2.5K) — Docker-owned repo. Official-ish.
- manutej/luxor-claude-marketplace@docker-compose-orchestration (3K) — popular, unknown.

## MCP servers
- Next.js: next-devtools-mcp (Vercel, official). Install: `npx -y next-devtools-mcp` in .mcp.json, or `npx add-mcp next-devtools-mcp@latest`. Needs Next.js 16+ dev server running; endpoint /_next/mcp. Credibility: official.
- PostgreSQL: crystaldba/postgres-mcp (Postgres MCP Pro). Run with `--access-mode=restricted` and use a read-only DB role; the server flag alone is not a guarantee. Credibility: popular, actively maintained; not vendor-official. The reference server (modelcontextprotocol, archived) is also read-only but simpler.
- Spring/Java: Spring AI MCP Server Boot Starter (spring-ai-starter-mcp-server-webmvc / -webflux / stdio). Official Spring docs. This builds an MCP server from your app, not a DB or API MCP client. Use only if you want to expose the backend to AI agents.
- Docker: docker/mcp-gateway (official Docker repo). Compose image `docker/mcp-gateway`, `--servers=...`. Credibility: official.

## Not found
- Official Java 25 skill, Hibernate/Spring Data official skill, openapi-typescript/openapi-fetch official skill.
