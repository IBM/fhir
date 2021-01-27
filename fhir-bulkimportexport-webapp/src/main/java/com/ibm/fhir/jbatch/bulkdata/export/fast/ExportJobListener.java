/*
 * (C) Copyright IBM Corp. 2021
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package com.ibm.fhir.jbatch.bulkdata.export.fast;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.batch.api.listener.JobListener;
import javax.batch.runtime.context.JobContext;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Dependent
public class ExportJobListener implements JobListener {
    private static final Logger logger = Logger.getLogger(ExportJobListener.class.getName());

    @Inject
    JobContext jobContext;

    public ExportJobListener() {
    }


    @Override
    public void afterJob() {

        // This list of partition summaries is compiled by the ExportPartitionAnalyzer and
        // injected into the jobContext as transient user data
        @SuppressWarnings("unchecked")
        List<PartitionSummary> partitionSummaries = (List<PartitionSummary>)jobContext.getTransientUserData();

        // If the job is stopped before any partition is finished, then nothing to show.
        if (partitionSummaries == null) {
            return;
        }

        List<String> resourceTypeSummaries = new ArrayList<>();
        for (PartitionSummary partitionSummary : partitionSummaries) {
            resourceTypeSummaries.add(partitionSummary.getResourceTypeSummary());
        }

        if (resourceTypeSummaries.size() > 0) {
            // e.g, Patient[1000,1000,200]:Observation[1000,250]
            String status = String.join(":", resourceTypeSummaries);
            logger.fine("Setting jobContext exit status: '" + status + "'");
            jobContext.setExitStatus(status);
        }
    }

    @Override
    public void beforeJob() {
        // NOP
    }
}