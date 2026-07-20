# FFmpeg estatico via imagem dedicada (nao ha pacote no repo padrao do
# AlmaLinux por causa de licenciamento de codec H.264/AAC). Puxar de uma
# imagem Docker em vez de baixar o tarball direto de um site de terceiros
# usa o registry do Docker Hub (rapido, cacheavel, resiliente) em vez de
# um download HTTP unico sujeito a throttling/lentidao de rede.
FROM mwader/static-ffmpeg:7.1 AS ffmpeg

FROM finlumia/base:finlumia-dev-almalinux10module_java21

COPY --from=ffmpeg /ffmpeg  /usr/local/bin/ffmpeg
COPY --from=ffmpeg /ffprobe /usr/local/bin/ffprobe

COPY docs/build/libs/*.jar /tmp/app/

EXPOSE 28082
