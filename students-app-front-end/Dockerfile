FROM node:18-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --legacy-peer-deps
COPY . .
ARG REACT_APP_ENV=dev
ARG REACT_APP_DEV_DOMAIN=localhost
ENV REACT_APP_ENV=$REACT_APP_ENV REACT_APP_DEV_DOMAIN=$REACT_APP_DEV_DOMAIN
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/build /usr/share/nginx/html
EXPOSE 80
