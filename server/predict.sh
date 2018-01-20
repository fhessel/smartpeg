#!/bin/bash

# Switch to the script directory
SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPTDIR"

# Check that our virtual environment does exist, if not, create it
if [ ! -d "env" ]; then
	echo "Virtual environment does not exist, creating..."
	virtualenv -p python3 env

	echo "Importing dependencies"
	source env/bin/activate
	pip install -r "requirements_raspberry.txt"
	deactivate
fi

# Activate the virtual environment
echo "Activating the virtual environment"
source env/bin/activate

# Run prediction
echo "Requesting prediction"
RESULT="$(python ./predict.py $1)"

# Deactivate the virtual environment
echo "Leaving virtual environment"
deactivate

echo "Done."
echo "PREDICTION=$RESULT"
