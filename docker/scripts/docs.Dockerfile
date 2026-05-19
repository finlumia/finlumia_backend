FROM finlumia/base:almalinux10-zulu21

COPY docs/build/libs/*.jar /tmp/app/

RUN set -eux; \
    APP_JAR=""; \
    for jar in /tmp/app/*.jar; do \
      case "$jar" in \
        *-plain.jar|*original*.jar) continue ;; \
      esac; \
      APP_JAR="$jar"; \
      break; \
    done; \
    [ -n "$APP_JAR" ]; \
    mv "$APP_JAR" /app.jar; \
    rm -rf /tmp/app

EXPOSE 40574

ENV PATH="/root/.sdkman/candidates/java/current/bin:/usr/lib/jvm/zulu21/bin:${PATH}"

ENTRYPOINT ["/bin/bash", "-lc", "if [ -x /root/.sdkman/candidates/java/current/bin/java ]; then exec /root/.sdkman/candidates/java/current/bin/java -jar /app.jar; elif [ -x /usr/lib/jvm/zulu21/bin/java ]; then exec /usr/lib/jvm/zulu21/bin/java -jar /app.jar; elif command -v java >/dev/null 2>&1; then exec java -jar /app.jar; else echo 'ERRO: java nao encontrado na imagem base' >&2; exit 127; fi"]
