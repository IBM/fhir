#!/usr/bin/env bash

# ----------------------------------------------------------------------------
# (C) Copyright IBM Corp. 2021
#
# SPDX-License-Identifier: Apache-2.0
# ----------------------------------------------------------------------------

CONFIG_ARG=""
if [ $CONFIG_PROPS ]; then
  CONFIG_ARG="-config $CONFIG_PROPS"
fi

if [ $LOAD_UMLS = 'true' ]; then
  echo 'Loading UMLS data into TermGraph'
  java -classpath "/opt/lib/*" -Xmx4G com.ibm.fhir.term.graph.loader.impl.UMLSTermGraphLoader $CONFIG_ARG
fi

if [ $LOAD_MAP = 'true' ]; then
  echo 'Loading Snomed to ICD Map Data into TermGraph'
  java -classpath "/opt/lib/*" -Xmx4G com.ibm.fhir.term.graph.loader.impl.SnomedICD10MapTermGraphLoader $CONFIG_ARG
fi