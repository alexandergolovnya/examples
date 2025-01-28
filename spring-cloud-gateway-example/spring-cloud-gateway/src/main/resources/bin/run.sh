#!/usr/bin/env bash
JMX_OPTS="-Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=9012 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.rmi.port=9012"

java -jar ${JMX_OPTS} ${JAVA_OPTS} ${APP_JAR}