FROM debian:jessie
MAINTAINER Zenreach <engineering@zenreach.com>

ENV SECOR_VERSION 0.26
ADD container/java.list /etc/apt/sources.list.d/

# Add Maven dependencies (not shaded into the artifact; Docker-cached)
RUN mkdir -p /opt/secor
RUN mkdir -p /tmp/secor_data

ENV JAVA_VERSION_MAJOR=8
ENV JAVA_VERSION_MINOR=181
ENV JAVA_VERSION_BUILD=13
ENV JAVA_DOWNLOAD_HASH=96a7b8442fe848ef90c96a2fad6ed6d1

RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886
RUN apt-get update && apt-get -y install wget

RUN mkdir -p /usr/lib/jvm \
  && cd /usr/lib/jvm \
  && wget -nv --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-b${JAVA_VERSION_BUILD}/${JAVA_DOWNLOAD_HASH}/jdk-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar.gz \
  && tar xf jdk-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar.gz \
  && rm jdk-${JAVA_VERSION_MAJOR}u${JAVA_VERSION_MINOR}-linux-x64.tar.gz \
  && update-alternatives --install "/usr/bin/java" "java" "/usr/lib/jvm/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/bin/java" 1 \
  && update-alternatives --install "/usr/bin/javac" "javac" "/usr/lib/jvm/jdk1.${JAVA_VERSION_MAJOR}.0_${JAVA_VERSION_MINOR}/bin/javac" 1

ENV JAVA_HOME=/usr/lib/jvm

EXPOSE 9990

# Add the service itself
ADD target/secor-*-bin.tar.gz /opt/secor/

COPY src/main/scripts/docker-entrypoint.sh /opt/secor/docker-entrypoint.sh
RUN chmod +x /opt/secor/docker-entrypoint.sh

WORKDIR /opt/secor

ENTRYPOINT ["/opt/secor/docker-entrypoint.sh"]

