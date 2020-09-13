#!/bin/bash

JAR_FILE=SessionService.jar
PROCESS_NAME=SessionManagementService

function printUsage {
	echo "Expecting start or stop command line argument."
	exit -1;
}

if [ "$#" -ne 1 ] 
then
	printUsage
fi

function killExisting {
	PS_OUTPUT=$(ps -ef | grep $PROCESS_NAME | grep -v grep)
	if [ "$PS_OUTPUT" == "" ]
	then
		echo "No existing $PROCESS_NAME found running."
	else
		PROCESS_ID=$(echo $PS_OUTPUT | awk '{print $2}')
		echo "Killing $PROCESS_NAME with ProcessId $PROCESS_ID"
		kill -9 $PROCESS_ID
	fi
} 

if [ "$1" == "start" ] || [ "$1" == "stop" ]  
then
	echo "Command issued to $1 $PROCESS_NAME"
	
	if [ "$1" == "start" ]
	then
		echo "First stopping any existing $PROCESS_NAME process."
		killExisting
		echo "Starting $PROCESS_NAME"
		java -Dprocess.name=$PROCESS_NAME -jar $JAR_FILE &
		exit 1;
	else
		killExisting
	fi
else
    printUsage
fi
