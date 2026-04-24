FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y --no-install-recommends openssl && rm -rf /var/lib/apt/lists/*

COPY scripts/gen-certs.sh /scripts/gen-certs.sh
RUN chmod +x /scripts/gen-certs.sh

ENV CERT_OUT_DIR=/out

ENTRYPOINT ["/scripts/gen-certs.sh"]
