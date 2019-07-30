FROM openjdk:8
MAINTAINER Zenreach <engineering@zenreach.com>

RUN rm /etc/ssl/certs/java/cacerts ; update-ca-certificates -f

ENV SECOR_VERSION 0.26

RUN mkdir -p /opt/secor
RUN mkdir -p /tmp/secor_data

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
RUN echo "deb [check-valid-until=no] http://archive.debian.org/debian jessie-backports main" > /etc/apt/sources.list.d/jessie-backports.list

RUN sed -i '/deb http:\/\/deb.debian.org\/debian jessie-updates main/d' /etc/apt/sources.list

RUN apt-get -o Acquire::Check-Valid-Until=false update && apt-get -y install wget

EXPOSE 9990

# Add the service itself
ADD target/secor-*-bin.tar.gz /opt/secor/

COPY src/main/scripts/docker-entrypoint.sh /opt/secor/docker-entrypoint.sh
RUN chmod +x /opt/secor/docker-entrypoint.sh

WORKDIR /opt/secor

ENTRYPOINT ["/opt/secor/docker-entrypoint.sh"]

