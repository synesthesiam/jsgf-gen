FROM alpine:latest
LABEL maintainer="Michael Hansen <hansen.mike@gmail.com>"

RUN apk update && \
    apk add openjdk8-jre

COPY build/install/jsgf-gen/bin/* /usr/bin/
COPY build/install/jsgf-gen/lib/* /usr/lib/

ENTRYPOINT ["/usr/bin/jsgf-gen"]
