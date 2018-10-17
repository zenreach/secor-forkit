FROM java:8
MAINTAINER Zenreach <engineering@zenreach.com>

ENV SECOR_VERSION 0.26
ADD container/java.list /etc/apt/sources.list.d/

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
RUN mkdir -p /opt/secor
RUN mkdir -p /tmp/secor_data

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
RUN apt-get update && apt-get -y install wget

EXPOSE 9990

# Add the service itself
ADD target/secor-*-bin.tar.gz /opt/secor/

COPY src/main/scripts/docker-entrypoint.sh /opt/secor/docker-entrypoint.sh
RUN chmod +x /opt/secor/docker-entrypoint.sh

WORKDIR /opt/secor

ENTRYPOINT ["/opt/secor/docker-entrypoint.sh"]

