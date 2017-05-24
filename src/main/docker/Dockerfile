FROM frolvlad/alpine-oraclejdk8:slim
MAINTAINER Maxime Dor <max@kamax.io>

EXPOSE 8091
VOLUME "/data"

ADD matrix-appservice-email.jar /mxas-email/mxas-email.jar

WORKDIR "/data"
ENTRYPOINT [ "/usr/bin/java" ]
CMD [ "-Djava.security.egd=file:/dev/urandom", "-jar", "/mxas-email/mxas-email.jar", "--spring.config.location=/data/" ]
