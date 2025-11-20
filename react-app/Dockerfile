# Multi-stage build for React frontend
# Use regular node (not alpine) for build to avoid Rollup issues with musl
FROM node:20 AS build

WORKDIR /app

# Build argument for API URL
ARG VITE_API_URL=https://api.jhaell.me/api
ENV VITE_API_URL=$VITE_API_URL

# Copy package files and install dependencies (cached layer)
COPY package*.json ./

# Workaround for npm optional dependencies bug with Rollup
# https://github.com/npm/cli/issues/4828
RUN rm -f package-lock.json && npm install

# Copy source code and build
COPY . .
RUN npm run build

# Production stage with nginx
FROM nginx:1.25-alpine

# Copy custom nginx config
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copy built files from build stage (Vite outputs to /build due to outDir: '../build')
COPY --from=build /build /usr/share/nginx/html

# Create a non-root user for security
RUN chown -R nginx:nginx /usr/share/nginx/html && \
    chown -R nginx:nginx /var/cache/nginx && \
    chown -R nginx:nginx /var/log/nginx && \
    touch /var/run/nginx.pid && \
    chown -R nginx:nginx /var/run/nginx.pid

USER nginx

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

CMD ["nginx", "-g", "daemon off;"]
