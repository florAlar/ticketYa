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
# Global mock default true; overrides finos para demock selectivo (como .env local)
ARG VITE_USE_MOCK=true
ARG VITE_USE_MOCK_AUTH=false
ARG VITE_USE_MOCK_EVENTOS=false
ARG VITE_USE_MOCK_COMPRAS=false
ARG VITE_USE_MOCK_ENTRADAS=false
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_USE_MOCK=$VITE_USE_MOCK
ENV VITE_USE_MOCK_AUTH=$VITE_USE_MOCK_AUTH
ENV VITE_USE_MOCK_EVENTOS=$VITE_USE_MOCK_EVENTOS
ENV VITE_USE_MOCK_COMPRAS=$VITE_USE_MOCK_COMPRAS
ENV VITE_USE_MOCK_ENTRADAS=$VITE_USE_MOCK_ENTRADAS
RUN npm run build

# Etapa 2: estáticos + conf nginx del monorepo
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY conf/nginx-default.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
