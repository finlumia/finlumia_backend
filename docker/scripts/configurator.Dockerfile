FROM finlumia/base:finlumia-dev-almalinux10module_java21

COPY configurator/build/libs/*.jar /tmp/app/

EXPOSE 28081
