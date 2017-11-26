#!/bin/bash

# Small helper tool for observing the raw telnet transmission of experimental data
#
# While data is recorded using
#   telnet <device-ip> 500 | tee output.csv
# it may happen that packets get lost and the retransmission buffer of the esp8266
# is not able to retransmit them.
# In this case, the output.csv is not appended anymore and values may get lost.
#
# Running this script in parallel allows to check that the file is appended at least every
# 10 seconds. If not, it prints and alarm to stdout and calls the beep program to issue
# an acoustic alarm. Under debian-like distributions, beep can be installed from default
# package sources (apt-get install beep).

# Sanity check of parameters
if [ "$1" == "" ]; then
	echo "Missing parameter. Call: $0 <file to observe>"
else

	echo "Observing $1"

	# Seconds without a change
	ALARMCOUNT=0

	NEWCHANGE=$(ls -la "$1")
	sleep 1

	while true; do
		LASTCHANGE=$NEWCHANGE
		NEWCHANGE=$(ls -la "$1")
		if [ "$LASTCHANGE" == "$NEWCHANGE" ]; then
			ALARMCOUNT=$(expr $ALARMCOUNT + 1)
		else
			ALARMCOUNT=0
		fi

		if (( $ALARMCOUNT > 10 )); then
			echo "ALARM! No update for $ALARMCOUNT seconds!"
			beep
		fi
		sleep 1
	done

fi
