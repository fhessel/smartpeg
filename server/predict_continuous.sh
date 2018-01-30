#!/bin/bash

# Switch to the script directory
SCRIPTDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$SCRIPTDIR"

# Activate the virtual environment
source env/bin/activate

# Run prediction
python ./predict_continuous.py

# Deactivate the virtual environment
deactivate

