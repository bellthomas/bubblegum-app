FROM openjdk:11-jre
COPY .tmp /app
EXPOSE 80 443
RUN ["chmod", "+x", "/app/bin/start"]
CMD ["/app/bin/start", "-Dhttp.port=80", "-Dhttps.port=443","-Dplay.crypto.secret=EIFNUPkFfZawMqZUwRbKi"]
