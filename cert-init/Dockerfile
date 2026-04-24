FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache openssl bash

COPY scripts/gen-certs.sh /scripts/gen-certs.sh
RUN chmod +x /scripts/gen-certs.sh

ENV CERT_OUT_DIR=/out

ENTRYPOINT ["/scripts/gen-certs.sh"]
