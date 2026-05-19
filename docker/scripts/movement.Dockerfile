FROM finlumia/base:almalinux10-zulu21
COPY movement/build/libs/*.jar /app.jar

EXPOSE 40573

ENV PATH="/root/.sdkman/candidates/java/current/bin:/usr/lib/jvm/zulu21/bin:${PATH}"

ENTRYPOINT ["/bin/bash", "-lc", "if [ -x /root/.sdkman/candidates/java/current/bin/java ]; then exec /root/.sdkman/candidates/java/current/bin/java -jar /app.jar; elif [ -x /usr/lib/jvm/zulu21/bin/java ]; then exec /usr/lib/jvm/zulu21/bin/java -jar /app.jar; elif command -v java >/dev/null 2>&1; then exec java -jar /app.jar; else echo 'ERRO: java nao encontrado na imagem base' >&2; exit 127; fi"]