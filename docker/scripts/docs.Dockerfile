FROM finlumia/base:finlumia-dev-almalinux10module_java21

COPY docs/build/libs/*.jar /tmp/app/

EXPOSE 28082
