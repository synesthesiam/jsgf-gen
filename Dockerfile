FROM alpine:latest
LABEL maintainer="Michael Hansen <hansen.mike@gmail.com>"

COPY build/install/jsgf-gen/bin/* /usr/bin/
COPY build/install/jsgf-gen/lib/* /usr/lib/

RUN apk update && \
    apk add openjdk8-jre

ENTRYPOINT ["/usr/bin/jsgf-gen"]
