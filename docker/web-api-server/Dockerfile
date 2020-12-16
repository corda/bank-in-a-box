FROM azul/zulu-openjdk:8u192

COPY clients/build/libs/clients-0.1.jar /clients.jar
ENTRYPOINT ["java","-jar","/clients.jar"]