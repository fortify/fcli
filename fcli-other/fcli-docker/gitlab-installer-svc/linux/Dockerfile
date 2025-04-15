FROM alpine:3

# Define environment variables
ENV SCRIPTS_DIR=/scripts
ENV FCLI_DIR=/fcli
ENV PATH=${PATH}:${SCRIPTS_DIR}
# Install & upgrade packages
RUN apk update && apk --no-cache add socat

ADD scripts/* $SCRIPTS_DIR/
ADD fcli $FCLI_DIR/fcli

EXPOSE 9999
ENTRYPOINT ["/bin/sh", "-c", "${SCRIPTS_DIR}/entrypoint-wrapper.sh"]
