#!/usr/bin/env bash
mvn install:install-file -Dfile=lib/tinyb.jar -DgroupId=intel-iot-devkit -DartifactId=tinyb -Dversion=0.6.0-SNAPSHOT -Dpackaging=jar
mvn install:install-file -Dfile=lib/com.zsmartsystems.bluetooth.bluegiga-1.0.0-SNAPSHOT.jar -DgroupId=com.zsmartsystems.bluetooth.bluegiga -DartifactId=com.zsmartsystems.bluetooth.bluegiga -Dversion=1.0.0-SNAPSHOT -Dpackaging=jar