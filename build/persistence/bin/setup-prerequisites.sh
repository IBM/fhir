#!/usr/bin/env bash

###############################################################################
# (C) Copyright IBM Corp. 2020
#
# SPDX-License-Identifier: Apache-2.0
###############################################################################
set -ex

# required_build - executes for every build
required_build(){
    # Clean up the packages and docker files not on the Mac
    if [[ "$OSTYPE" != "darwin"* ]]
    then
        sudo apt clean
        docker rmi $(docker image ls -aq)
        df -h
    fi

    # build binaries
    mvn -B install --file fhir-examples --no-transfer-progress
    mvn -B install --file fhir-parent -DskipTests -P include-fhir-igs,integration --no-transfer-progress

    # Build from dockerfile
    docker build fhir-install --build-arg VERBOSE=false -t linuxforhealth/fhir-server

     echo "Building fhir-bulkdata-server image!!!!"
     docker build fhir-install-bulkdata --build-arg VERBOSE=false -t linuxforhealth/fhir-bulkdata-server
}

# persistence_build - executes for each persistence build triggering the persistence layer's required steps.
persistence_build(){
    PERSISTENCE="${1}"
    if [ -f "build/persistence/${PERSISTENCE}/setup-prerequisites.sh" ]
    then
        echo "Running [${PERSISTENCE}] setting setup prerequisites"
        bash build/persistence/${PERSISTENCE}/setup-prerequisites.sh
    fi
}

###############################################################################
# Store the current directory to reset to
pushd $(pwd) > /dev/null

if [ -z "${WORKSPACE}" ]
then
    echo "The WORKSPACE value is unset"
    exit -1
fi

# Change to the release directory
cd "${WORKSPACE}"

required_build
persistence_build "${1}"

# Reset to Original Directory
popd > /dev/null

# EOF
###############################################################################
