#!/bin/bash
# Helper script that can be called by the cron job
# It handles storing the logfiles depending on the outcome of the deployment.
# This reduces the effort spent for logging in the main script.

cd /home/smartpeg/cron/autodeploy-smartpeg-api/
export HOME=/home/smartpeg

./run.sh > last_run.log 2>&1
RUN_ERROR=$?


cd /home/smartpeg/cron/autodeploy-smartpeg-api/

if (( $RUN_ERROR > 0 )); then
	cp last_run.log last_error.log
fi
