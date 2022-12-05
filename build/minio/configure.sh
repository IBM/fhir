#!/usr/bin/env bash
###############################################################################
# (C) Copyright IBM Corp. 2016, 2022
#
# SPDX-License-Identifier: Apache-2.0
###############################################################################

if [[ -z "${WORKSPACE}" ]]; then
    echo "ERROR: WORKSPACE environment variable not set!"
    exit 1
fi

CONFIG="${WORKSPACE}/build/minio/fhir-server/config"
TEST_RESOURCES="${WORKSPACE}/fhir-server-test/src/test/resources"

# Set the fhir-server-config
echo "Copying the fhir server config files..."
cp ${CONFIG}/default/fhir-server-config-postgresql-minio.json ${CONFIG}/default/fhir-server-config.json
# debug: validate the config files
echo "validate the config files in config directory..."
ls ${CONFIG}

echo "validate the config files in configDropins directory..."
ls ${CONFIG}/configDropins/defaults/

echo "validate the config files in overrides directory..."
ls ${CONFIG}/configDropins/overrides/

cat ${CONFIG}/server.xml

# Enable the file-based import/export tests and set the path to the output dir
sed -i -e 's/test.bulkdata.export.enabled = false/test.bulkdata.export.enabled = true/g' ${TEST_RESOURCES}/test.properties
sed -i -e 's/test.bulkdata.import.enabled = false/test.bulkdata.import.enabled = true/g' ${TEST_RESOURCES}/test.properties
sed -i -e "s:test.bulkdata.path = .*:test.bulkdata.path = ${WORKSPACE}/build/minio/fhir-server/bulkdata:" ${TEST_RESOURCES}/test.properties

# Enable the S3-based import/export tests
sed -i -e 's/test.bulkdata.import.s3.enabled = false/test.bulkdata.import.s3.enabled = true/g' ${TEST_RESOURCES}/test.properties
sed -i -e 's/test.bulkdata.export.s3.enabled = false/test.bulkdata.export.s3.enabled = true/g' ${TEST_RESOURCES}/test.properties

S3_BULKDATA="${WORKSPACE}/build/minio/minio/miniodata/bulkdata/"
mkdir -p ${S3_BULKDATA}
cp ${TEST_RESOURCES}/testdata/import-operation/test-import.ndjson ${S3_BULKDATA}
cp ${TEST_RESOURCES}/testdata/import-operation/test-import-skip.ndjson ${S3_BULKDATA}
cp ${TEST_RESOURCES}/testdata/import-operation/test-import-neg.ndjson ${S3_BULKDATA}
cp ${TEST_RESOURCES}/testdata/import-operation/test-import-mismatch.ndjson ${S3_BULKDATA}