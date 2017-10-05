#!/bin/bash

function containerIp () {
	ip=$(docker inspect --format=\"{{.NetworkSettings.Networks."$COMPOSE_PROJECT_NAME"_elastest.IPAddress}}\" "$COMPOSE_PROJECT_NAME"_$1_1 2> /dev/null)
	error=$?
	if [ -z "$2" ]; then
		echo $( echo $ip | cut -f2 -d'"' )
	elif [ "$2" = 'check' ]; then
		echo $error
	fi
}

projectName="elastest"

