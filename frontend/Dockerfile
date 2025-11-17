# Multi-stage build for React application

# Build stage
FROM node:18-alpine AS build
WORKDIR /app

# Build argument for API URL
ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=${REACT_APP_API_URL}

# Copy package files
COPY package*.json ./

# Install dependencies with cache mount for faster rebuilds
# --prefer-offline: 캐시된 패키지 우선 사용
# --legacy-peer-deps: peer dependency 충돌 해결
RUN --mount=type=cache,target=/root/.npm \
    npm install --prefer-offline --no-audit --progress=false --legacy-peer-deps

# Copy source code
COPY . .

# Build application
RUN npm run build

# Runtime stage with nginx
FROM nginx:1.25-alpine
WORKDIR /usr/share/nginx/html

# Remove default nginx files
RUN rm -rf ./*

# Copy built files from build stage
COPY --from=build /app/build .

# Copy nginx configuration
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expose port
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost/ || exit 1

# Start nginx
CMD ["nginx", "-g", "daemon off;"]

