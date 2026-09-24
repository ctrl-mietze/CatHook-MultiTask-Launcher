FROM node:22-bookworm-slim
WORKDIR /app
COPY relay-src-0.3.2.tar.gz /tmp/relay-src.tar.gz
RUN tar -xzf /tmp/relay-src.tar.gz -C /app \
 && npm install --omit=dev --no-audit --no-fund \
 && rm -f /tmp/relay-src.tar.gz
ENV NODE_ENV=production
CMD ["node","src/server.mjs"]
