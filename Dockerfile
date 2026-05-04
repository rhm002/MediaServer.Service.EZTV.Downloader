# Execute Command
# docker rmi eztv-downloader-service
# docker build -t rhm002/media-server:eztv-downloader-service-latest -f ./MediaServer.Service.EZTV.Downloader/Dockerfile .
# docker run --name eztv-downloader-service --rm rhm002/media-server:eztv-downloader-service-latest
# docker run --name eztv-downloader-service --rm -e SPRING_DATASOURCE_URL="jdbc:sqlserver://192.168.1.8:1433;DatabaseName=MediaServer;encrypt=false" -e SPRING_DATASOURCE_USERNAME=media -e SPRING_DATASOURCE_PASSWORD= rhm002/media-server:eztv-downloader-service-latest
# docker run --name eztv-downloader-service --rm -it rhm002/media-server:eztv-downloader-service-latest bash
# docker exec -it eztv-downloader-service bash
# docker image push rhm002/media-server --all-tags

FROM rhm002/openjdk:jre-25-trixie

LABEL maintainer="Ronald Mundell <rhm002@gmail.com>"

ARG SPRING_PROFILES_ACTIVE=k8s-psql

RUN mkdir -p /media \
    && useradd -d /media --uid=1000 media \
    && echo 'media:media' | chpasswd \
    && chown -R media:media /media

USER media
WORKDIR /media

ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}

COPY target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
