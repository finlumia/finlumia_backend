##############################################################
# nginx.Dockerfile — Imagem Nginx para o Finlumia
##############################################################
FROM nginx:1.27-alpine

# Remove config padrão do Nginx
RUN rm /etc/nginx/conf.d/default.conf

# Copia toda a configuração customizada
COPY docker/nginx/nginx.conf        /etc/nginx/nginx.conf
COPY docker/nginx/conf.d/           /etc/nginx/conf.d/
COPY docker/nginx/snippets/         /etc/nginx/snippets/

# Diretório para o desafio Let's Encrypt (certbot)
RUN mkdir -p /var/www/certbot

# Testa a configuração na build (falha se nginx.conf tiver erro)
RUN nginx -t

EXPOSE 80 443
