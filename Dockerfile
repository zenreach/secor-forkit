FROM debian:jessie
MAINTAINER Zenreach <engineering@zenreach.com>

ADD container/java.list /etc/apt/sources.list.d/

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
RUN mkdir -p /opt/secor
RUN mkdir -p /tmp/secor_data

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
RUN apt-get update && \
    echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections && \
    DEBIAN_FRONTEND=noninteractive apt-get install -y -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confnew" \
    git oracle-java8-installer oracle-java8-set-default runit unzip && \
    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

EXPOSE 9990

# Add the service itself
ADD target/secor-*-bin.tar.gz /opt/secor/

COPY src/main/scripts/docker-entrypoint.sh /opt/secor/docker-entrypoint.sh
RUN chmod +x /opt/secor/docker-entrypoint.sh

WORKDIR /opt/secor

ENTRYPOINT ["/opt/secor/docker-entrypoint.sh"]

