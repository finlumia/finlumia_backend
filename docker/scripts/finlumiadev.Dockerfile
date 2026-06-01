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

# Instalação do JDK 21
RUN rpm --import https://repos.azul.com/azul-repo.key && \
    rpm -ivh https://cdn.azul.com/zulu/bin/zulu-repo-1.0.0-1.noarch.rpm && \
    microdnf install -y zulu21-jdk && \
    microdnf clean all && \
    rm -rf /var/cache/microdnf

# Definição das variáveis de ambiente do JDK 21
ENV JAVA_HOME=/usr/lib/jvm/zulu21
ENV PATH="${JAVA_HOME}/bin:${PATH}"

# Instalação do Docker CLI  e Buildx plugin para permitir a construção de imagens docker dentro 
#do container, o que e util para o desenvolvimento e testes do projeto finlumia,
#alem de facilitar a integracao com o Docker Hub ou outros registries de container.
RUN curl -Lo /etc/yum.repos.d/docker-ce.repo \
    https://download.docker.com/linux/rhel/docker-ce.repo && \
    microdnf install -y docker-ce-cli docker-buildx-plugin && \
    microdnf clean all && \
    rm -rf /var/cache/microdnf

# Limpeza final dos caches para reduzir o tamanho da imagem final
RUN rm -rf \
    # Limpeza dos caches do microdnf e dnf 
    /var/cache/microdnf \
    /var/cache/dnf \
    /var/lib/dnf \
    # Limpeza dos repositórios de instalação
    /etc/yum.repos.d/azul.repo \
    /etc/yum.repos.d/docker-ce.repo \
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

RUN useradd -m -s /bin/bash finlumia \
    && mkdir -p /home/finlumia/.gradle \
    && chown -R finlumia:finlumia /home/finlumia

EXPOSE 28080

CMD ["tail", "-f", "/dev/null"]

