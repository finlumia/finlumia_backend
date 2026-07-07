FROM finlumia/base:finlumia-dev-almalinux10module_java21

COPY support/build/libs/*.jar /tmp/app/

EXPOSE 28086
