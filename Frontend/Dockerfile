# Frontend TicketYa (Vite → Nginx)
# Etapa 1: build
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# En imagen, el browser del usuario sigue pegándole al gateway publicado en el host
# (o vía /api si se usa same-origin detrás de este nginx).
ARG VITE_API_BASE_URL=http://localhost:8080
# Único interruptor de mock: false = todo contra el gateway real; true = todo mock local (demo offline)
ARG VITE_USE_MOCK=false
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_USE_MOCK=$VITE_USE_MOCK
RUN npm run build

# Etapa 2: estáticos + conf nginx del monorepo
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY conf/nginx-default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
