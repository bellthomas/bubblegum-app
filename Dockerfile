FROM openjdk:11-jre
COPY .tmp /app
COPY scripts/start.sh /bubblegum.sh
EXPOSE 80 443
RUN ["chmod", "+x", "/app/bin/start"]
RUN ["chmod", "+x", "/bubblegum.sh"]

ENV FRP_VERSION 0.26.0

RUN set -x && \
    wget --no-check-certificate https://github.com/fatedier/frp/releases/download/v${FRP_VERSION}/frp_${FRP_VERSION}_linux_amd64.tar.gz && \
    tar xzf frp_${FRP_VERSION}_linux_amd64.tar.gz && \
    cd frp_${FRP_VERSION}_linux_amd64 && \
    mkdir /frp && \
    mv frpc /frpc && \
    cd .. && \
    rm -rf *.tar.gz && \
    rm -rf frp_${FRP_VERSION}_linux_amd64

COPY scripts/frpc.ini /
CMD ["/bubblegum.sh"]
