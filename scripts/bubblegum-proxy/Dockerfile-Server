FROM alpine:latest
COPY frps/ /proxy/
RUN ["chmod", "+x", "/proxy/frps"]
CMD ["/proxy/frps", "-c", "/proxy/frps.ini"]
