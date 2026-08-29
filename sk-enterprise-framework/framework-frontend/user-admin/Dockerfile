# syntax=docker/dockerfile:1
# =====================================================================
# 프론트엔드(user-admin) 컨테이너: Vite 빌드 → Nginx 정적 서빙 + /api 프록시
# =====================================================================

FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
