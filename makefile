HOST=192.168.7.2
DEBUG_PORT=4455
JMX_PORT=4456
JARNAME=helios.jar
DIR=./helios


debug:
	java -Dcom.sun.management.jmxremote \
		-Dcom.sun.management.jmxremote.authenticate=false \
		-Dcom.sun.management.jmxremote.ssl=false \
		-Dcom.sun.management.jmxremote.port=${JMX_PORT} \
		-Djava.rmi.server.hostname=192.168.7.2 \
		-Xdebug -Xrunjdwp:server=y,transport=dt_socket,address=${DEBUG_PORT},suspend=n -jar ${JARNAME}

release:
	java -jar ${JARNAME}
