#!/usr/bin/env bash
###############################################################################
# (C) Copyright IBM Corp. 2016, 2021
#
# SPDX-License-Identifier: Apache-2.0
###############################################################################

# Exit the script if any commands fail
set -e
# Print each command before executing it
#set -x
# This allows subshells to inheret the options above
export SHELLOPTS

# The full path to the directory of this script, no matter where its called from
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
export WORKSPACE="$( dirname "${DIR}" )"

if [ -z "${1}" ]; then
  echo "This script requires an argument that points to one of the integration environment folders."
  exit 1
elif [ -z "$(find ${DIR} -maxdepth 1 -name ${1} -type d)" ]; then
  echo "'${1}' is not a valid argument. This script requires an argument that points to one of the integration environment folders."
  exit 1
fi

subdir="${1}"
echo "Bringing down environment for the ${subdir} fhir-server integration tests..."

# Log output locations
it_results=${WORKSPACE}/integration-test-results
zip_file=${WORKSPACE}/integration-test-results.zip

echo "Clearing out any existing pre-it test logs..."
rm -rf ${it_results} 2>/dev/null
mkdir -p ${it_results}/server-logs
mkdir -p ${it_results}/fhir-server-test

containerId=$(docker ps -a | grep ${subdir}[-_]fhir-server | cut -d ' ' -f 1)
if [ -z "${containerId}" ]; then
    echo "Warning: Could not find fhir-server container!!!"
else
    echo "fhir container id: $containerId"

    # Grab the container's console log
    docker logs $containerId  >& ${it_results}/docker-console.txt

    echo "Gathering pre-test server logs from docker container: $containerId"
    docker cp -L $containerId:/logs ${it_results}/server-logs
fi

echo "Gathering integration test output"
if [ -e ${WORKSPACE}/fhir-server-test/target/surefire-reports ]; then
    cp -r ${WORKSPACE}/fhir-server-test/target/surefire-reports/* ${it_results}/fhir-server-test
fi

# Source the tenant1 datastore variables; not strictly needed here
# but avoids "variable is not set" docker compose warnings
. ${WORKSPACE}/build/common/set-tenant1-datastore-vars.sh

echo "Bringing down the fhir server docker container(s)..."
cd ${DIR}/${subdir}
docker compose down

echo "Integration test post-processing completed!"
