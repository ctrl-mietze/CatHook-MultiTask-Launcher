FROM node:22-bookworm-slim
WORKDIR /app

COPY app/package.json /app/package.json
RUN npm install --omit=dev --no-audit --no-fund

COPY app/ /app/

RUN node --check /app/src/server.mjs \
 && node --check /app/src/store.mjs \
 && node --check /app/src/mcp.mjs \
 && node --check /app/src/deviceHub.mjs

ENV NODE_ENV=production
CMD ["node","src/server.mjs"]
