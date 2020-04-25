#!/bin/sh

debug="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
properties="-Dorg.regadou.jhux.debug=true"

java $debug $properties -jar target/jhux-jar-with-dependencies.jar $@

