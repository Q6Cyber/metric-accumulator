FROM azul-zulu:17
COPY metrics-accumulator.yml /etc/metrics-accumulator/metrics-accumulator.yml
COPY target/app.jar /
ENTRYPOINT ["java","-jar","/app.jar"]
CMD ["--config-file=/etc/metrics-accumulator/metrics-accumulator.yml"]