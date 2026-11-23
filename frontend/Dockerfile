FROM nginx:stable-alpine

COPY frontend/dist/ /usr/share/nginx/html/
COPY media/ /usr/share/nginx/html/media/
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
