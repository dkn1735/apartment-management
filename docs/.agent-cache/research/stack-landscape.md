# Stack landscape (as of 2026-11-16)

Budget used: 5 web searches, 8 page fetches. UNCONFIRMED = not verified against an official source this run.

1. Spring Boot
- Latest GA line: 4.1 (latest patch 4.1.2, 22 Oct 2026; line date 30 Jun 2026 per endoflife.date). A search snippet gave 10 Jun 2026 for 4.1.0, so the date conflicts. Freshness: fetched 2026-11-16.
  https://endoflife.date/spring-boot  |  https://spring.io/projects/spring-boot
- Spring Boot 4.2: UNCONFIRMED (secondary sources expected Nov 2026).
- Java baseline: 17 per secondary sources (UNCONFIRMED on official page). 4.1 features need Java 21 (per secondary sources). Recommend Java 21 or 25.
- Spring Boot 3.5: OSS support ENDED 30 Jun 2026 (last patch 3.5.16, 25 Jun 2026). Commercial support to 2032: UNCONFIRMED.
- Spring Boot 4.0: OSS support ends 31 Dec 2026 (latest patch 4.0.9). Do not start greenfield on 4.0; use 4.1.

2. Java LTS
- Java 21 and 25 are LTS: from prior knowledge, UNCONFIRMED this run (endoflife.date/java fetch returned no version data).
  Official: https://www.oracle.com/java/technologies/java-se-support-roadmap.html (not fetched)

3. Next.js
- Major line: 16. Exact latest patch: UNCONFIRMED (search results gave 16.2.x to 16.4.1, conflicting). Check with `npm show next version`.
- Minimum Node.js: 20.9 (official docs, lastUpdated 2026-10-02).
- React: install command is `react@latest react-dom@latest`; App Router bundles React canary with the stable React 19 changes. Exact React version: UNCONFIRMED.
  https://nextjs.org/docs/app/getting-started/installation
- Turbopack is the default dev/build bundler in the current docs.

4. Node.js LTS (endoflife.date, fetched 2026-11-16)
- Active LTS: 26 (latest 26.13.1, 11 Nov 2026; active support to 27 Oct 2027).
- Maintenance LTS: 24 (24.22.3; active support ended 20 Oct 2026; security to 30 Apr 2028), 22 (22.23.5; security to 30 Apr 2027).
  https://endoflife.date/nodejs
- Note: orval 8.48.0 requires Node >=22.18.0.

5. PostgreSQL
- Latest GA major: 18 (latest patch 18.7; EOL 14 Nov 2030). Version 19 is not listed as released; a search claimed GA on 19 Nov 2026, so UNCONFIRMED.
- PostgreSQL 14 EOL: 12 Nov 2026 (already past or at today's date).
  https://endoflife.date/postgresql  |  https://www.postgresql.org/support/versioning/

6. Migrations: Flyway vs Liquibase
- Flyway: latest 13.3.0 and Spring Boot 4.1 manages 12.4.0. Both come only from one secondary blog, so UNCONFIRMED. The same source says Spring Boot 4 moved Flyway auto-config into a `spring-boot-flyway` module (UNCONFIRMED).
- Flyway licensing: Community is listed as free and supports PostgreSQL (fetched red-gate.com page). The page does not state license terms, and no PostgreSQL-specific license change was found. Flyway 10 dropped the paid-license requirement for old DB versions and moved license keys out of the OSS edition.
  https://www.red-gate.com/products/flyway/community/  |  https://flywaydb.org/documentation/learnmore/releaseNotes
- Liquibase status: UNCONFIRMED (not searched, budget).

7. springdoc-openapi
- Line for Spring Boot 4 is 3.x; Spring Boot 3 uses 2.8.x. Newest found: 3.1.0 (targets Spring Boot 4.1.0). Source was a release mirror only, so UNCONFIRMED on the official releases page.
  https://github.com/springdoc/springdoc-openapi/releases
- Pin the springdoc minor to your Spring Boot minor.

8. Client generation from OpenAPI
- openapi-typescript: 7.13.0 (npm registry, fetched). Generates types only. No engines field.
  https://www.npmjs.com/package/openapi-typescript
- orval: 8.48.0 (npm registry, fetched). Requires Node >=22.18.0.
  https://www.npmjs.com/package/orval  |  https://orval.dev (not fetched)
- openapi-fetch (runtime client paired with openapi-typescript): version UNCONFIRMED (not checked).
