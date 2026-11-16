# Build bằng node:24 với output standalone, chạy `node server.js`, user không phải root
FROM node:24 AS build
WORKDIR /app
RUN npm i -g pnpm@12.17.0
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
# --ignore-scripts bỏ script prepare (lefthook install cần .git, image không có); các
# dependency có build script đều đã tắt bằng allowBuilds trong pnpm-workspace.yaml
RUN pnpm install --frozen-lockfile --ignore-scripts
COPY . .
# rewrites được đóng cứng lúc build: image chỉ chạy với backend tên "backend" trong compose
ARG BACKEND_URL=http://backend:8080
ENV BACKEND_URL=${BACKEND_URL}
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm build

FROM node:24-slim
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
COPY --from=build --chown=node:node /app/.next/standalone ./
COPY --from=build --chown=node:node /app/.next/static ./.next/static
USER node
EXPOSE 3000
CMD ["node", "server.js"]
