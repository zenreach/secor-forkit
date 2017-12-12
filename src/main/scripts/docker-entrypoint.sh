#!/bin/bash
set -e

cd /opt/secor

DEFAULT_CLASSPATH="*:lib/*"
CLASSPATH=${CLASSPATH:-$DEFAULT_CLASSPATH}

java -Xmx${JVM_MEMORY:-512m} $JAVA_OPTS -ea -cp $CLASSPATH com.zenreach.data.secor.ArchiverMain $@
