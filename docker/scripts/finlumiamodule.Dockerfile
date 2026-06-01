# Script responsavel pela criacao de uma imagem docker almalinux para o projeto da
# finlumia, portanto o objetivo e montar um ambiente de desenvolvimento pronto
# para utilização no projeto sem a necessidade configurações e instalações de dependências
# A imagem é baseada na imagem oficial do almalinux, que é uma distribuição linux

FROM almalinux:10.1-minimal-20260509

# Instalação de dependências necessárias para o desenvolvimento do projeto,
# como git, tar, wget, curl e shadow-utils
RUN microdnf install -y \
    git tar wget curl \
    shadow-utils \
    && microdnf clean all

# Instalação do Zulu JRE 21 (headless)
#
# zulu21-jre-headless: JRE sem componentes gráficos (AWT/Swing),
# suficiente para executar aplicações Spring Boot e reduz ~100MB
# em relação ao zulu21-jdk.
RUN rpm --import https://repos.azul.com/azul-repo.key && \
    rpm -ivh https://cdn.azul.com/zulu/bin/zulu-repo-1.0.0-1.noarch.rpm && \
    microdnf install -y zulu21-jre-headless && \
    microdnf clean all && \
    rm -rf /var/cache/microdnf

# Definição das variáveis de ambiente do JRE 21
ENV JAVA_HOME=/usr/lib/jvm/zulu21
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Limpeza final dos caches para reduzir o tamanho da imagem final
RUN rm -rf \
    # Limpeza dos caches do microdnf e dnf 
    /var/cache/microdnf \
    /var/cache/dnf \
    /var/lib/dnf \
    # Limpeza dos repositórios de instalação
    /etc/yum.repos.d/azul.repo \
    # Documentações e man pages
    /usr/share/doc \
    /usr/share/man \
    /usr/share/info \
    # Locales desnecessários
    /usr/share/locale \
    # Logs
    /var/log/* \
    # Temporários
    /tmp/* \
    /root/.bash_history

RUN useradd -m -s /bin/bash finlumia

# Script de entrypoint centralizado: seleciona o JAR correto em /tmp/app/,
# ignorando artefatos secundários do Gradle (-plain.jar, *original*.jar),
# e executa via java. Os módulos filhos só precisam fazer COPY *.jar /tmp/app/.
RUN printf '#!/bin/sh\nset -e\nAPP_JAR=""\nfor jar in /tmp/app/*.jar; do\n  case "$jar" in\n    *-plain.jar|*original*.jar) continue ;;\n  esac\n  APP_JAR="$jar"\n  break\ndone\n[ -n "$APP_JAR" ] || { echo "ERRO: nenhum JAR executavel encontrado em /tmp/app/" >&2; exit 1; }\nexec java -jar "$APP_JAR"\n' > /docker-entrypoint.sh && \
    chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]

